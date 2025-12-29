/*
 * #%L
 * PistonQueue
 * %%
 * Copyright (C) 2021 AlexProgrammerDE
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package net.pistonmaster.pistonqueue.shared.command;

import net.pistonmaster.pistonqueue.shared.chat.ComponentWrapper;
import net.pistonmaster.pistonqueue.shared.chat.ComponentWrapperFactory;
import net.pistonmaster.pistonqueue.shared.chat.MessageType;
import net.pistonmaster.pistonqueue.shared.chat.TextColorWrapper;
import net.pistonmaster.pistonqueue.shared.chat.TextDecorationWrapper;
import net.pistonmaster.pistonqueue.shared.config.Config;
import net.pistonmaster.pistonqueue.shared.plugin.PistonQueuePlugin;
import net.pistonmaster.pistonqueue.shared.utils.StorageTool;
import net.pistonmaster.pistonqueue.shared.wrapper.CommandSourceWrapper;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;
import net.pistonmaster.pistonqueue.shared.wrapper.ServerInfoWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class MainCommandSharedTest {

  private static Path tempDir;
  private TestCommandHandler handler;
  private TestCommandSender sender;
  private TestPlugin plugin;

  @BeforeAll
  static void setupStorage() throws IOException {
    tempDir = Files.createTempDirectory("pq-command-test");
    StorageTool.setupTool(tempDir);
  }

  @BeforeEach
  void setUp() {
    Config config = new Config();
    config.copyFrom(config);
    plugin = new TestPlugin(config);
    handler = new TestCommandHandler();
    sender = new TestCommandSender(true); // Admin by default
  }

  @Test
  void helpCommandShowsCommands() {
    handler.onCommand(sender, new String[]{"help"}, plugin);

    assertTrue(sender.hasReceivedMessage("PistonQueue"));
    assertTrue(sender.hasReceivedMessage("/pq help"));
    assertTrue(sender.hasReceivedMessage("/pq version"));
    assertTrue(sender.hasReceivedMessage("/pq stats"));
  }

  @Test
  void helpCommandShowsAdminCommandsForAdmins() {
    handler.onCommand(sender, new String[]{"help"}, plugin);

    assertTrue(sender.hasReceivedMessage("/pq slotstats"));
    assertTrue(sender.hasReceivedMessage("/pq reload"));
    assertTrue(sender.hasReceivedMessage("/pq shadowban"));
  }

  @Test
  void helpCommandHidesAdminCommandsForNonAdmins() {
    sender = new TestCommandSender(false);
    handler.onCommand(sender, new String[]{"help"}, plugin);

    assertFalse(sender.hasReceivedMessage("/pq slotstats"));
    assertFalse(sender.hasReceivedMessage("/pq reload"));
    assertFalse(sender.hasReceivedMessage("/pq shadowban"));
  }

  @Test
  void versionCommandShowsVersion() {
    handler.onCommand(sender, new String[]{"version"}, plugin);

    assertTrue(sender.hasReceivedMessage("Version test-version by"));
  }

  @Test
  void shadowbanWithValidDaysDuration() {
    handler.onCommand(sender, new String[]{"shadowban", "TestPlayer", "2d"}, plugin);

    assertTrue(sender.hasReceivedMessage("Successfully shadowbanned TestPlayer!"));
    assertTrue(StorageTool.isShadowBanned("TestPlayer"));

    // Cleanup
    StorageTool.unShadowBanPlayer("TestPlayer");
  }

  @Test
  void shadowbanWithValidHoursDuration() {
    handler.onCommand(sender, new String[]{"shadowban", "TestPlayer", "5h"}, plugin);

    assertTrue(sender.hasReceivedMessage("Successfully shadowbanned TestPlayer!"));
    assertTrue(StorageTool.isShadowBanned("TestPlayer"));

    // Cleanup
    StorageTool.unShadowBanPlayer("TestPlayer");
  }

  @Test
  void shadowbanWithValidMinutesDuration() {
    handler.onCommand(sender, new String[]{"shadowban", "TestPlayer", "30m"}, plugin);

    assertTrue(sender.hasReceivedMessage("Successfully shadowbanned TestPlayer!"));
    assertTrue(StorageTool.isShadowBanned("TestPlayer"));

    // Cleanup
    StorageTool.unShadowBanPlayer("TestPlayer");
  }

  @Test
  void shadowbanWithValidSecondsDuration() {
    handler.onCommand(sender, new String[]{"shadowban", "TestPlayer", "60s"}, plugin);

    assertTrue(sender.hasReceivedMessage("Successfully shadowbanned TestPlayer!"));
    assertTrue(StorageTool.isShadowBanned("TestPlayer"));

    // Cleanup
    StorageTool.unShadowBanPlayer("TestPlayer");
  }

  @Test
  void shadowbanWithUnknownSuffixShowsHelp() {
    // "10x" has no valid suffix (d, h, m, s), so it shows help
    handler.onCommand(sender, new String[]{"shadowban", "TestPlayer", "10x"}, plugin);

    // Should show help instead of crashing
    assertTrue(sender.hasReceivedMessage("/pq shadowban player <d|h|m|s>"));
    assertFalse(StorageTool.isShadowBanned("TestPlayer"));
  }

  @Test
  void shadowbanWithInvalidNumberThrowsNumberFormatException() {
    // BUG: When suffix is valid but number is invalid, it throws NumberFormatException
    // "abcd" ends with 'd' so it tries to parse "abc" as integer
    // This test documents the bug - ideally it should show help instead
    assertThrows(NumberFormatException.class, () ->
      handler.onCommand(sender, new String[]{"shadowban", "TestPlayer", "abcd"}, plugin)
    );
  }

  @Test
  void shadowbanWithNegativeNumberWorks() {
    // Negative numbers are parsed but result in past expiry (immediately expires)
    // This documents current behavior
    assertDoesNotThrow(() ->
      handler.onCommand(sender, new String[]{"shadowban", "TestPlayer", "-5d"}, plugin)
    );

    // Cleanup if it was somehow banned
    StorageTool.unShadowBanPlayer("TestPlayer");
  }

  @Test
  void shadowbanWithMissingPlayerNameShowsHelp() {
    handler.onCommand(sender, new String[]{"shadowban"}, plugin);

    assertTrue(sender.hasReceivedMessage("/pq shadowban player <d|h|m|s>"));
  }

  @Test
  void shadowbanWithMissingDurationShowsHelp() {
    handler.onCommand(sender, new String[]{"shadowban", "TestPlayer"}, plugin);

    assertTrue(sender.hasReceivedMessage("/pq shadowban player <d|h|m|s>"));
  }

  @Test
  void shadowbanAlreadyBannedPlayerShowsError() {
    // First ban
    StorageTool.shadowBanPlayer("TestPlayer", java.util.Date.from(java.time.Instant.now().plusSeconds(3600)));

    handler.onCommand(sender, new String[]{"shadowban", "TestPlayer", "1d"}, plugin);

    assertTrue(sender.hasReceivedMessage("TestPlayer is already shadowbanned!"));

    // Cleanup
    StorageTool.unShadowBanPlayer("TestPlayer");
  }

  @Test
  void unshadowbanExistingBan() {
    StorageTool.shadowBanPlayer("TestPlayer", java.util.Date.from(java.time.Instant.now().plusSeconds(3600)));

    handler.onCommand(sender, new String[]{"unshadowban", "TestPlayer"}, plugin);

    assertTrue(sender.hasReceivedMessage("Successfully unshadowbanned TestPlayer!"));
    assertFalse(StorageTool.isShadowBanned("TestPlayer"));
  }

  @Test
  void unshadowbanNonBannedPlayerShowsError() {
    handler.onCommand(sender, new String[]{"unshadowban", "NotBanned"}, plugin);

    assertTrue(sender.hasReceivedMessage("NotBanned is not shadowbanned!"));
  }

  @Test
  void unshadowbanWithMissingPlayerNameShowsHelp() {
    handler.onCommand(sender, new String[]{"unshadowban"}, plugin);

    assertTrue(sender.hasReceivedMessage("/pq unshadowban player"));
  }

  @Test
  void adminCommandsRequirePermission() {
    sender = new TestCommandSender(false);

    handler.onCommand(sender, new String[]{"slotstats"}, plugin);
    assertTrue(sender.hasReceivedMessage("You do not"));

    sender.clearMessages();
    handler.onCommand(sender, new String[]{"reload"}, plugin);
    assertTrue(sender.hasReceivedMessage("You do not"));

    sender.clearMessages();
    handler.onCommand(sender, new String[]{"shadowban", "Test", "1d"}, plugin);
    assertTrue(sender.hasReceivedMessage("You do not"));
  }

  @Test
  void unknownCommandShowsHelp() {
    handler.onCommand(sender, new String[]{"unknowncommand"}, plugin);

    assertTrue(sender.hasReceivedMessage("/pq help"));
  }

  @Test
  void tabCompletionReturnsCommands() {
    List<String> completions = handler.onTab(new String[]{""}, sender, plugin);

    assertTrue(completions.contains("help"));
    assertTrue(completions.contains("version"));
    assertTrue(completions.contains("stats"));
  }

  @Test
  void tabCompletionFiltersCommands() {
    List<String> completions = handler.onTab(new String[]{"h"}, sender, plugin);

    assertTrue(completions.contains("help"));
    assertFalse(completions.contains("version"));
  }

  @Test
  void tabCompletionIncludesAdminCommandsForAdmins() {
    List<String> completions = handler.onTab(new String[]{""}, sender, plugin);

    assertTrue(completions.contains("slotstats"));
    assertTrue(completions.contains("reload"));
    assertTrue(completions.contains("shadowban"));
  }

  @Test
  void tabCompletionExcludesAdminCommandsForNonAdmins() {
    sender = new TestCommandSender(false);
    List<String> completions = handler.onTab(new String[]{""}, sender, plugin);

    assertFalse(completions.contains("slotstats"));
    assertFalse(completions.contains("reload"));
    assertFalse(completions.contains("shadowban"));
  }

  @Test
  void caseInsensitiveCommandParsing() {
    handler.onCommand(sender, new String[]{"HELP"}, plugin);
    assertTrue(sender.hasReceivedMessage("/pq help"));

    sender.clearMessages();
    handler.onCommand(sender, new String[]{"HeLp"}, plugin);
    assertTrue(sender.hasReceivedMessage("/pq help"));
  }

  @Test
  void caseInsensitiveDurationSuffix() {
    handler.onCommand(sender, new String[]{"shadowban", "TestPlayer", "1D"}, plugin);
    assertTrue(sender.hasReceivedMessage("Successfully shadowbanned TestPlayer!"));
    StorageTool.unShadowBanPlayer("TestPlayer");

    handler.onCommand(sender, new String[]{"shadowban", "TestPlayer2", "1H"}, plugin);
    assertTrue(sender.hasReceivedMessage("Successfully shadowbanned TestPlayer2!"));
    StorageTool.unShadowBanPlayer("TestPlayer2");
  }

  // Test helper classes

  private static class TestCommandHandler implements MainCommandShared {
    @Override
    public ComponentWrapperFactory component() {
      return TestComponentWrapper::new;
    }
  }

  private static class TestComponentWrapper implements ComponentWrapper {
    private final StringBuilder text = new StringBuilder();

    TestComponentWrapper(String text) {
      this.text.append(text);
    }

    @Override
    public ComponentWrapper append(String text) {
      this.text.append(text);
      return this;
    }

    @Override
    public ComponentWrapper append(ComponentWrapper component) {
      this.text.append(((TestComponentWrapper) component).text);
      return this;
    }

    @Override
    public ComponentWrapper color(TextColorWrapper color) {
      return this;
    }

    @Override
    public ComponentWrapper decorate(TextDecorationWrapper decoration) {
      return this;
    }

    @Override
    public String toString() {
      return text.toString();
    }
  }

  private static class TestCommandSender implements CommandSourceWrapper {
    private final boolean isAdmin;
    private final List<String> receivedMessages = new ArrayList<>();

    TestCommandSender(boolean isAdmin) {
      this.isAdmin = isAdmin;
    }

    @Override
    public void sendMessage(ComponentWrapper component) {
      receivedMessages.add(component.toString());
    }

    @Override
    public boolean hasPermission(String node) {
      if ("queue.admin".equals(node)) {
        return isAdmin;
      }
      return false;
    }

    boolean hasReceivedMessage(String substring) {
      return receivedMessages.stream().anyMatch(msg -> msg.contains(substring));
    }

    void clearMessages() {
      receivedMessages.clear();
    }
  }

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "CT_CONSTRUCTOR_THROW",
    justification = "Test class does not need protection from finalizer attacks"
  )
  private static class TestPlugin implements PistonQueuePlugin {
    private final Config config;
    private final Path dataDir;

    TestPlugin(Config config) {
      this.config = config;
      try {
        this.dataDir = Files.createTempDirectory("pq-test-plugin");
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public Optional<PlayerWrapper> getPlayer(UUID uuid) {
      return Optional.empty();
    }

    @Override
    public List<PlayerWrapper> getPlayers() {
      return List.of();
    }

    @Override
    public Optional<ServerInfoWrapper> getServer(String name) {
      return Optional.empty();
    }

    @Override
    public void schedule(Runnable runnable, long delay, long period, TimeUnit unit) {
    }

    @Override
    public void info(String message) {
    }

    @Override
    public void warning(String message) {
    }

    @Override
    public void error(String message) {
    }

    @Override
    public List<String> getAuthors() {
      return List.of("TestAuthor");
    }

    @Override
    public String getVersion() {
      return "test-version";
    }

    @Override
    public Path getDataDirectory() {
      return dataDir;
    }

    @Override
    public Config getConfiguration() {
      return config;
    }
  }
}
