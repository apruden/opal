/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.web.datashield.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public class DataShieldROptionsScriptBuilder {

  private Iterable<Map.Entry<String, String>> rOptions;

  private DataShieldROptionsScriptBuilder() {}

  public static DataShieldROptionsScriptBuilder newBuilder() {
    return new DataShieldROptionsScriptBuilder();
  }

  public DataShieldROptionsScriptBuilder setROptions(Iterable<Map.Entry<String, String>> options) {
    rOptions = options;
    return this;
  }

  public String build() {
    if (!rOptions.iterator().hasNext()) return "";

    List<String> options = new ArrayList<>();
    for(Map.Entry<String, String> entry : rOptions) {
      options.add(entry.getKey() + "=" + formatValue(entry.getValue()));
    }

    return String.format("options(%s)", StringUtils.collectionToCommaDelimitedString(options));
  }

  private String formatValue(String value) {
    String trimmedValue = value.trim();
    if (isNumeric(trimmedValue) || isBoolean(trimmedValue) || isNull(trimmedValue)) {
      return trimmedValue;
    }

    return quoteTextValue(trimmedValue);
  }

  private boolean isNumeric(String value) {
    Scanner scanner = new Scanner(value);
    return scanner.hasNextInt() || scanner.hasNextFloat();
  }

  private boolean isBoolean(String value) {
    Pattern pattern = Pattern.compile("^(TRUE|T|FALSE|F)$");
    return pattern.matcher(value).matches();
  }

  private boolean isNull(String value) {
    Pattern pattern = Pattern.compile("^NULL$");
    return pattern.matcher(value).matches();
  }

  private String quoteTextValue(String value) {
    Pattern pattern = Pattern.compile("^\\w+\\(");
    if (pattern.matcher(value).find()) {
      // R Options may contain directives or functions ('c', 'q', 'quote'), do not quote these
      return value;
    }

    return StringUtils.quote(StringUtils.trimTrailingCharacter((StringUtils.trimLeadingCharacter(value, '\'')), '\''));
  }
}

