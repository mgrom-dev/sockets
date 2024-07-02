package ru.mgrom.JSONS;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class MessageRequest extends AbstractRequest {

  public static final String TYPE = "sendMessage";

  private String recipient;
  private String message;

  {
    setType(TYPE);
  }
}
