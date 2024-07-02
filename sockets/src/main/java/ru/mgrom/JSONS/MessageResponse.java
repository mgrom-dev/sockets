package ru.mgrom.JSONS;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class MessageResponse extends AbstractRequest {

  public static final String TYPE = "userMessage";

  private String from;
  private String message;

  {
    setType(TYPE);
  }
}
