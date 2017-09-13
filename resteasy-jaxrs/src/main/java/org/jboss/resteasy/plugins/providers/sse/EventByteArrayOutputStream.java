package org.jboss.resteasy.plugins.providers.sse;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class EventByteArrayOutputStream extends ByteArrayOutputStream
{
   private void removeBlankLine()
   {
      if (this.count > 4)
      {
         count = count - 4;
      }
   }

   public synchronized byte[] getEventPayLoad()
   {
      removeBlankLine();
      return Arrays.copyOf(buf, count);
   }
   
   public synchronized byte[] getEventData()
   {
      if (buf[count] == '\n')
      {
         return Arrays.copyOf(buf, count - 1);
      }
      return Arrays.copyOf(buf, count);
   }
}
