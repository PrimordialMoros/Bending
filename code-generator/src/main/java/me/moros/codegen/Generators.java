package me.moros.codegen;

import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;

public final class Generators {
  private static final Logger LOGGER = System.getLogger("Code Gen Logger");
  private static final String PKG = "me.moros.bending.api.platform.";
  private static final String VERSION = "1_19_2";

  public static void main(String[] args) {
    if (args.length != 1) {
      LOGGER.log(Level.ERROR, "Usage: <target folder>");
      return;
    }

    var generator = new CodeGenerator(LOGGER, Path.of(args[0]), "v" + VERSION.replace("_", "."));
    generator.generate(resource("blocks"), pkg("block"), "BlockType");
    generator.generate(resource("entities"), pkg("entity"), "EntityType");
    generator.generate(resource("items"), pkg("item"), "Item");
    generator.generate(resource("particles"), pkg("particle"), "Particle");
    generator.generate(resource("potion_effects"), pkg("potion"), "PotionEffect");
    generator.generate(resource("sounds"), pkg("sound"), "Sound");

    generator.generate(tagResource("block_tags"), pkg("block"), "BlockTag", "TagImpl", "Tags");
    generator.generate(tagResource("item_tags"), pkg("item"), "ItemTag", "TagImpl", "Tags");

    LOGGER.log(Level.INFO, "Finished generating code");
  }

  private static String pkg(String packageString) {
    return PKG + packageString;
  }

  private static InputStream tagResource(String name) {
    return resource("tags/" + VERSION + "_" + name);
  }

  private static InputStream resource(String name) {
    return Generators.class.getResourceAsStream("/" + VERSION + "_" + name + ".json");
  }
}
