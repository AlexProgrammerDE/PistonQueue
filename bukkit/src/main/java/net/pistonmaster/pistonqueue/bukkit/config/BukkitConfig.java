package net.pistonmaster.pistonqueue.bukkit.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;

@Configuration
public final class BukkitConfig {
  @Comment("Force the user to remain in a certain position.")
  public LocationSection location = new LocationSection();

  @Comment(
    "Visibility controls. Hiding players also disables join/leave messages."
  )
  public VisibilitySection visibility = new VisibilitySection();

  @Comment("Options that prevent communication while waiting in queue.")
  public CommunicationSection communication = new CommunicationSection();

  @Comment("Audio feedback played when the proxy sends plugin messages.")
  public AudioSection audio = new AudioSection();

  @Comment("General player protection toggles applied while in queue.")
  public ProtectionsSection protections = new ProtectionsSection();

  @Comment({ "ProtocolLib specific options", "Only applied when ProtocolLib is present" })
  public ProtocolLibSection protocolLib = new ProtocolLibSection();

  @Configuration
  public static final class LocationSection {
    @Comment("Force the user to remain in a certain position.")
    public boolean enabled = true;

    @Comment("Forced world")
    public String world = "world_the_end";

    @Comment("Forced coordinates")
    public CoordinatesSection coordinates = new CoordinatesSection();
  }

  @Configuration
  public static final class CoordinatesSection {
    @Comment("Forced x coordinate")
    public int x = 500;

    @Comment("Forced y coordinate")
    public int y = 256;

    @Comment("Forced z coordinate")
    public int z = 500;
  }

  @Configuration
  public static final class VisibilitySection {
    @Comment(
      "Hide players from each other so that it looks like every user is alone in the world."
    )
    public boolean hidePlayers = true;

    @Comment("Prevent players from moving.")
    public boolean restrictMovement = true;

    @Comment("Force players to remain in a gamemode.")
    public ForceGamemodeSection forceGamemode = new ForceGamemodeSection();

    @Comment("Show a player's own name to themselves in spectator menu.")
    public TeamSection team = new TeamSection();
  }

  @Configuration
  public static final class ForceGamemodeSection {
    @Comment("Force players to remain in a gamemode.")
    public boolean enabled = true;

    @Comment("The gamemode to force players to remain in.")
    public String mode = "spectator";
  }

  @Configuration
  public static final class TeamSection {
    @Comment("Show a player's own name to themselves in spectator menu.")
    public boolean enabled = false;

    @Comment("The team name the user sees. (Valid placeholders: %player_name%, %random%)")
    public String name = "%player_name%";
  }

  @Configuration
  public static final class CommunicationSection {
    @Comment("Don't allow players to chat.")
    public boolean disableChat = true;

    @Comment("Don't allow commands.")
    public boolean disableCommands = true;
  }

  @Configuration
  public static final class AudioSection {
    @Comment("Plays an XP sound when the proxy sends a plugin message.")
    public boolean playXpSound = true;
  }

  @Configuration
  public static final class ProtectionsSection {
    @Comment("Prevents players from gaining experience.")
    public boolean preventExperience = true;

    @Comment("Prevents players from getting damage.")
    public boolean preventDamage = true;

    @Comment("Prevents players from gaining hunger.")
    public boolean preventHunger = true;
  }

  @Configuration
  public static final class ProtocolLibSection {
    @Comment("Doesn't show the client's position on F3.")
    public boolean disableDebug = true;

    @Comment("Packets that should be suppressed when ProtocolLib is installed.")
    public SuppressPacketsSection suppressPackets = new SuppressPacketsSection();

    @Comment(
      "Does not send entity metadata anymore, causing that the entire player head is shown while in spectator."
    )
    public boolean showFullHead = true;
  }

  @Configuration
  public static final class SuppressPacketsSection {
    @Comment("Do not send chunk data packets.")
    public boolean chunk = true;

    @Comment("Do not send time packets.")
    public boolean time = true;

    @Comment("Do not send health packets.")
    public boolean health = true;

    @Comment("Do not send advancement packets.")
    public boolean advancement = true;

    @Comment("Do not send experience packets.")
    public boolean experience = true;
  }
}
