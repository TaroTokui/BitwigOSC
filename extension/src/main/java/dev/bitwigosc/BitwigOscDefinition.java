package dev.bitwigosc;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class BitwigOscDefinition extends ControllerExtensionDefinition
{
   static final String VERSION = "0.5.0";
   private static final UUID ID = UUID.fromString("5f1dc41c-33a6-425a-a61a-863d0ce5eff7");

   @Override public String getName() { return "BitwigOSC"; }
   @Override public String getAuthor() { return "BitwigOSC"; }
   @Override public String getVersion() { return VERSION; }
   @Override public UUID getId() { return ID; }
   @Override public int getRequiredAPIVersion() { return 25; }
   @Override public String getHardwareVendor() { return "BitwigOSC"; }
   @Override public String getHardwareModel() { return "TouchOSC"; }
   @Override public int getNumMidiInPorts() { return 0; }
   @Override public int getNumMidiOutPorts() { return 0; }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
      final PlatformType platformType)
   {
   }

   @Override
   public BitwigOscExtension createInstance(final ControllerHost host)
   {
      return new BitwigOscExtension(this, host);
   }
}
