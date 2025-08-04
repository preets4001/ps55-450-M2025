package common;


import java.io.Serializable;

public class Payload implements Serializable {
   private PayloadType payloadType;
   private long clientId;
   private String message;

   public Payload() {
   }

   public Payload(PayloadType var1) {
      this.payloadType = var1;
   }

   public PayloadType getPayloadType() {
      return this.payloadType;
   }

   public void setPayloadType(PayloadType var1) {
      this.payloadType = var1;
   }

   public long getClientId() {
      return this.clientId;
   }

   public void setClientId(long var1) {
      this.clientId = var1;
   }

   public String getMessage() {
      return this.message;
   }

   public void setMessage(String var1) {
      this.message = var1;
   }

   public String toString() {
      return String.format("Payload[type=%s, clientId=%d, message=%s]", this.payloadType, this.clientId, this.message);
   }
}
