package kz.greetgo.conf.spring.cloud.hot;

import java.nio.file.Paths;

public class TestHotConfig {
  public static void main(String[] args) {
    {
      TestCloudHotConfigFab fab = new TestCloudHotConfigFab(Paths.get("build").resolve("hotAsd"), ".hotconfig");

      HotConfig1 config1 = fab.createConfig1();
      HotConfig2 config2 = fab.createConfig2();

      printValues(config1, config2);
    }
    {
      TestCloudHotConfigFromFile fab = new TestCloudHotConfigFromFile(Paths.get("build").resolve("hotAsd"), ".hotconfig");

      HotConfig1 config1 = fab.createConfig1();
      HotConfig2 config2 = fab.createConfig2();

      printValues(config1, config2);
    }
  }

  private static void printValues(HotConfig1 config1, HotConfig2 config2) {
    System.out.println("----------------------------------------------------");
    System.out.println("config1.strExampleValue = " + config1.strExampleValue());
    System.out.println("config1.intExampleValue = " + config1.intExampleValue());
    System.out.println("config1.boolExampleValue = " + config1.boolExampleValue());
    System.out.println("config2.asd = " + config2.asd());
    System.out.println("config2.intAsd = " + config2.intAsd());
  }
}