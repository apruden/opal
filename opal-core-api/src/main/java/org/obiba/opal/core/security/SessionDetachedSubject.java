/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.core.security;

import com.google.common.collect.Lists;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.PermissionResolver;
import org.apache.shiro.authz.permission.PermissionResolverAware;
import org.apache.shiro.mgt.AuthorizingSecurityManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.SessionKey;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.obiba.shiro.realm.DetachableRealm;

import java.util.List;

/**
 * Shiro's DelegatingSubject impl that is not tied to a session, but only to the principals of a given original session.
 */
public class SessionDetachedSubject extends DelegatingSubject {

  SessionDetachedSubject(final DelegatingSubject source, final SecurityManager securityManager) {
    super(source.getPrincipals(), source.isAuthenticated(), null, null, source.getSecurityManager()); //securityManager);
  }

  @Override
  public void login(AuthenticationToken token) throws AuthenticationException {
    //no login allowed
  }

  @Override
  public void logout() {
    //no logout possible
  }

  @Override
  protected boolean isSessionCreationEnabled() {
    return false; //no session creation allowed
  }

  /**
   * Returns a session detached subject. This makes sure the job is immune to sessiontimeouts/logouts (OPAL-2717)
   *
   * @param original
   * @return session detached Subject
   */
  public static Subject asSessionDetachedSubject(final Subject original) {
    if(original.getSession(false) != null && original instanceof DelegatingSubject) {
      //only creates a detached subject if has a session and is a DelegatingSubject
/*      final List<AuthorizationInfo> authorizationInfos = extractAuthorizationInfo((DelegatingSubject) original);
      AuthorizingSecurityManager securityManagerWrapper = new AuthorizingSecurityManager() {
        @Override
        public boolean isPermitted(PrincipalCollection principals, String permissionString) {
          try {
            return ((DelegatingSubject) original).getSecurityManager().isPermitted(principals, permissionString);
          } catch (AuthenticationException e) {
            return this.getAuthorizer().isPermitted(principals, permissionString);
          }
        }

        @Override
        public boolean isPermitted(PrincipalCollection principals, Permission permission) {
          try {
            return ((DelegatingSubject) original).getSecurityManager().isPermitted(principals, permission);
          } catch (AuthenticationException e) {
            return this.getAuthorizer().isPermitted(principals, permission);
          }
        }

        @Override
        public Session start(SessionContext sessionContext) {
          throw new UnsupportedOperationException();
        }

        @Override
        public Session getSession(SessionKey sessionKey) throws SessionException {
          throw new UnsupportedOperationException();
        }

        @Override
        public Subject login(Subject subject, AuthenticationToken authenticationToken) throws AuthenticationException {
          throw new UnsupportedOperationException();
        }

        @Override
        public void logout(Subject subject) {
          throw new UnsupportedOperationException();
        }

        @Override
        public Subject createSubject(SubjectContext subjectContext) {
          throw new UnsupportedOperationException();
        }
      };

      securityManagerWrapper.setAuthorizer(new DetachedAuthorizingRealm(authorizationInfos, new OpalPermissionResolver()));*/

      return new SessionDetachedSubject((DelegatingSubject) original, null); //securityManagerWrapper);
    }

    return original;
  }

  private static List<AuthorizationInfo> extractAuthorizationInfo(DelegatingSubject original) {
    List<AuthorizationInfo> authorizationInfos = Lists.newArrayList();
    DefaultSecurityManager sm = (DefaultSecurityManager) original.getSecurityManager();

    for(Realm realm : sm.getRealms()) {
      if(realm instanceof DetachableRealm) {
        authorizationInfos.add(((DetachableRealm)realm).getAuthorizationInfo(original.getPrincipals()));
      }
    }

    return authorizationInfos;
  }


  private static class DetachedAuthorizingRealm extends AuthorizingRealm implements
      PermissionResolverAware {
    private List<AuthorizationInfo> authorizationInfos;

    DetachedAuthorizingRealm(List<AuthorizationInfo> authorizationInfos, PermissionResolver permissionResolver) {
      this.authorizationInfos = authorizationInfos;
      this.setPermissionResolver(permissionResolver);
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
      throw new UnsupportedOperationException();
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPermitted(PrincipalCollection principals, Permission permission) {
      for(AuthorizationInfo authorizationInfo: authorizationInfos) {
        boolean[] res = isPermitted(Lists.newArrayList(permission), authorizationInfo);

        if(res.length > 0 && res[0]) return true;
      }

      return false;
    }
  }
}
