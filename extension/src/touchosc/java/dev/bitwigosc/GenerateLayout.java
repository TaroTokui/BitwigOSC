package dev.bitwigosc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** {@code ./gradlew generateLayout} の入口。引数のパスに TouchOSC のレイアウトを書き出す。 */
final class GenerateLayout
{
   private GenerateLayout()
   {
   }

   public static void main(final String[] args) throws IOException
   {
      if (args.length != 1)
      {
         System.err.println("usage: GenerateLayout <output.tosc>");
         System.exit(2);
      }
      final Path output = Path.of(args[0]).toAbsolutePath().normalize();
      Files.createDirectories(output.getParent());
      Files.write(output, ToscWriter.toTosc(MixerLayout.layout()));
      System.out.println("wrote " + output);
   }
}
