/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.security;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Set;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.shiro.mgt.SessionsSecurityManager;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.DefaultOptionsMethodException;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.util.IsHttpMethod;
import org.obiba.opal.core.service.SubjectAclService;
import org.obiba.opal.core.service.SubjectAclService.SubjectType;
import org.obiba.opal.web.ws.inject.RequestAttributesProvider;
import org.obiba.opal.web.ws.intercept.RequestCyclePostProcess;
import org.obiba.opal.web.ws.intercept.RequestCyclePreProcess;
import org.obiba.opal.web.ws.security.AuthorizeResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

@Component
public class AuthorizationInterceptor extends AbstractSecurityComponent implements RequestCyclePreProcess, RequestCyclePostProcess, ExceptionMapper<DefaultOptionsMethodException> {

  private static final Logger log = LoggerFactory.getLogger(AuthorizationInterceptor.class);

  private static final String ALLOW_HTTP_HEADER = "Allow";

  private final SubjectAclService subjectAclService;

  private final RequestAttributesProvider requestAttributeProvider;

  @Autowired
  public AuthorizationInterceptor(SessionsSecurityManager securityManager, SubjectAclService subjectAclService, RequestAttributesProvider requestAttributeProvider) {
    super(securityManager);
    this.subjectAclService = subjectAclService;
    this.requestAttributeProvider = requestAttributeProvider;
  }

  @Override
  public Response preProcess(HttpRequest request, ResourceMethod resourceMethod) {
    if(isWebServicePublic(resourceMethod) == false && isWebServiceWithoutAuthorization(resourceMethod) == false) {
      if(getSubject().isPermitted("magma:" + getResourceMethodUri(request, resourceMethod) + ":" + request.getHttpMethod()) == false) {
        return Response.status(Status.FORBIDDEN).build();
      }
    }
    return null;
  }

  @Override
  public void postProcess(HttpRequest request, ResourceMethod resourceMethod, ServerResponse response) {
    if(HttpMethod.GET.equals(request.getHttpMethod())) {
      Set<String> allowed = allowed(request, resourceMethod);
      if(allowed != null && allowed.size() > 0) {
        response.getMetadata().add(ALLOW_HTTP_HEADER, asHeader(allowed));
      }
    } else if(HttpMethod.DELETE.equals(request.getHttpMethod())) {
      subjectAclService.deleteNodePermissions("magma", request.getUri().getPath());
    }

    if(response.getStatus() == 201) {
      // Add permissions
      URI resourceUri = (URI) response.getMetadata().getFirst(HttpHeaders.LOCATION);
      if(resourceUri == null) {
        throw new IllegalStateException("Missing Location header in 201 response");
      }
      List<?> altLocations = response.getMetadata().get("X-Alt-Location");

      Iterable<URI> locations = ImmutableList.of(resourceUri);
      if(altLocations != null) {
        locations = Iterables.concat(locations, (List<URI>) altLocations);
      }
      addPermission(locations);
    }
  }

  @Override
  public Response toResponse(DefaultOptionsMethodException exception) {
    Response response = exception.getResponse();
    // Extract the Allow header generated by RestEASY. This contains all the methods of the resource class for the given
    // path
    String availableMethods = (String) response.getMetadata().getFirst(ALLOW_HTTP_HEADER);
    UriInfo uri = requestAttributeProvider.getUriInfo();
    return allow(allowed(uri.getPath(), ImmutableSet.of(availableMethods.split(", "))));
  }

  protected String getResourceMethodUri(HttpRequest request, ResourceMethod resourceMethod) {
    UriInfo info = request.getUri();
    if(resourceMethod.getMethod().isAnnotationPresent(AuthorizeResource.class)) {
      return info.getPath();
    } else {
      return info.getPath();
    }
  }

  private void addPermission(Iterable<URI> resourceUris) {
    for(URI resourceUri : resourceUris) {
      String resource = requestAttributeProvider.getResourcePath(resourceUri);
      if(getSubject().isPermitted("magma:" + resource + ":*") == false) {
        subjectAclService.addSubjectPermission("magma", resource, SubjectType.USER.subjectFor(getSubject().getPrincipal().toString()), "*:GET/*");
      }
    }
  }

  private Response allow(Set<String> allowed) {
    return Response.ok().header(ALLOW_HTTP_HEADER, asHeader(allowed)).build();
  }

  private String asHeader(Iterable<String> values) {
    StringBuilder sb = new StringBuilder();
    for(String s : values) {
      if(sb.length() > 0) sb.append(", ");
      sb.append(s);
    }
    return sb.toString();
  }

  /**
   * Returns a {@code Set} of allowed HTTP methods on the provided resource URI. Note that this method always includes
   * "OPTIONS" in the set.
   * @param uri
   * @param availableMethods
   * @return
   */
  private Set<String> allowed(final String uri, Set<String> availableMethods) {
    return ImmutableSet.copyOf(Iterables.concat(Iterables.filter(availableMethods, new Predicate<String>() {

      @Override
      public boolean apply(String from) {
        String perm = "magma:" + uri + ":" + from;
        boolean permitted = getSubject().isPermitted(perm);
        log.debug("isPermitted({}, {})=={}", new Object[] { getSubject().getPrincipal(), perm, permitted });
        return permitted;
      }

    }), ImmutableSet.of(HttpMethod.OPTIONS)));
  }

  private Set<String> allowed(final HttpRequest request, ResourceMethod method) {
    return allowed(request.getUri().getPath(), availableMethods(method));
  }

  private Set<String> availableMethods(ResourceMethod method) {
    Set<String> availableMethods = Sets.newHashSet();
    String path = getPath(method);
    for(Method otherMethod : method.getResourceClass().getMethods()) {
      Set<String> patate = IsHttpMethod.getHttpMethods(otherMethod);
      if(patate != null && isSamePath(otherMethod, path)) {
        availableMethods.addAll(patate);
      }
    }
    return availableMethods;
  }

  private String getPath(ResourceMethod method) {
    return getPath(method.getMethod());
  }

  private String getPath(Method method) {
    Path path = method.getAnnotation(Path.class);
    return path != null ? path.value() : "";
  }

  private boolean isSamePath(Method otherMethod, String path) {
    return path.equals(getPath(otherMethod));
  }
}
