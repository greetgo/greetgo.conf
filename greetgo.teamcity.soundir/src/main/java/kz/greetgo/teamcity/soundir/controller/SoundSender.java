package kz.greetgo.teamcity.soundir.controller;

import static kz.greetgo.teamcity.soundir.configs.BuildTypeMessages.getMessageFor;

import java.util.ArrayList;
import java.util.List;

import kz.greetgo.teamcity.soundir.configs.BuildType;
import kz.greetgo.teamcity.soundir.configs.BuildTypeComp;
import kz.greetgo.teamcity.soundir.configs.Comp;
import kz.greetgo.teamcity.soundir.player.Play;

public class SoundSender implements Runnable {
  private final BuildType buildType;
  private Finisher finisher;
  
  public static SoundSender around(BuildType buildType) {
    return new SoundSender(buildType);
  }
  
  public void go() {
    new Thread(this).start();
  }
  
  public SoundSender(BuildType buildType) {
    this.buildType = buildType;
  }
  
  public SoundSender with(Finisher finisher) {
    this.finisher = finisher;
    return this;
  }
  
  private class PlayRun implements Runnable {
    private final Comp comp;
    
    public PlayRun(Comp comp) {
      this.comp = comp;
    }
    
    @Override
    public void run() {
      Play.message(getMessageFor(buildType).text).to(comp.name());
    }
  }
  
  @Override
  public void run() {
    List<Thread> threads = new ArrayList<>();
    for (Comp comp : BuildTypeComp.data.get(buildType)) {
      threads.add(new Thread(new PlayRun(comp)));
    }
    
    for (Thread t : threads) {
      t.start();
    }
    
    for (Thread t : threads) {
      try {
        t.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    
    if (finisher != null) finisher.finish();
  }
}