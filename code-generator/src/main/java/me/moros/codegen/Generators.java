package me.moros.codegen;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;

import me.moros.codegen.vanilla.Generator;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.Main;
import net.minecraft.server.Bootstrap;

public final class Generators {
  private static final String BASE_PKG = "me.moros.bending.api.platform";

  public static final Logger LOGGER = System.getLogger("Code Gen Logger");
  public static final Path DATA_FOLDER;
  public static final Path TAGS_FOLDER;

  static {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();

    // Create a temp file and run vanilla data generator for tags
    Path tempDir;
    try {
      tempDir = Files.createTempDirectory("mojang_gen_data");
      Main.main(new String[]{"--server", "--output=" + tempDir});
      DATA_FOLDER = tempDir.resolve("data").resolve("minecraft");
      TAGS_FOLDER = DATA_FOLDER.resolve("tags");
    } catch (IOException e) {
      LOGGER.log(Level.ERROR, "Something went wrong while running Mojang's data generator.", e);
      throw new RuntimeException("Couldn't run the generator");
    }
  }

  public static void main(String[] args) {
    if (args.length != 2) {
      LOGGER.log(Level.ERROR, "Usage: <target folder> <version>");
      return;
    }

    final Path output = Path.of(args[0]);
    final String version = "v" + args[1];

    var generator = new ApiGenerator(BASE_PKG, output, version);

    generator.generate(from(BuiltInRegistries.BLOCK), "block", "BlockType");
    generator.generate(from(BuiltInRegistries.ENTITY_TYPE), "entity", "EntityType");
    generator.generate(from(BuiltInRegistries.ITEM), "item", "Item");
    generator.generate(from(BuiltInRegistries.PARTICLE_TYPE), "particle", "Particle");
    generator.generate(from(BuiltInRegistries.MOB_EFFECT), "potion", "PotionEffect");
    generator.generate(from(BuiltInRegistries.SOUND_EVENT), "sound", "Sound");

    generator.generate(tagsFrom("blocks"), "block", "Tags", "BlockTag", "TagImpl");
    generator.generate(tagsFrom("items"), "item", "Tags", "ItemTag", "TagImpl");

    LOGGER.log(Level.INFO, "Finished generating code");
  }

  private static <T> Generator from(Registry<T> registry) {
    return Generator.registry(registry);
  }

  private static Generator tagsFrom(String path) {
    return Generator.tag(TAGS_FOLDER.resolve(path));
  }
}
