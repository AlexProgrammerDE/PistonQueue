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
package net.pistonmaster.pistonqueue.shared.queue.logic;

import net.pistonmaster.pistonqueue.shared.config.Config;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;
import net.pistonmaster.pistonqueue.shared.queue.QueueType.QueueReason;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueueAvailabilityCalculatorTest {

  @Test
  void getFreeSlotsReturnsCorrectCount() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(10);
    QueueType type = config.QUEUE_TYPES[0];
    type.setReservedSlots(10);
    type.getPlayersWithTypeInTarget().set(3);

    QueueAvailabilityCalculator calculator = new QueueAvailabilityCalculator();

    int freeSlots = calculator.getFreeSlots(type);

    assertEquals(7, freeSlots);
  }

  @Test
  void getFreeSlotsReturnsZeroWhenFull() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    QueueType type = config.QUEUE_TYPES[0];
    type.setReservedSlots(5);
    type.getPlayersWithTypeInTarget().set(5);

    QueueAvailabilityCalculator calculator = new QueueAvailabilityCalculator();

    int freeSlots = calculator.getFreeSlots(type);

    assertEquals(0, freeSlots);
  }

  @Test
  void isTargetFullReturnsTrueWhenNoFreeSlots() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    QueueType type = config.QUEUE_TYPES[0];
    type.setReservedSlots(5);
    type.getPlayersWithTypeInTarget().set(5);

    QueueAvailabilityCalculator calculator = new QueueAvailabilityCalculator();

    boolean isFull = calculator.isTargetFull(type);

    assertTrue(isFull);
  }

  @Test
  void isTargetFullReturnsFalseWhenHasFreeSlots() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(10);
    QueueType type = config.QUEUE_TYPES[0];
    type.setReservedSlots(10);
    type.getPlayersWithTypeInTarget().set(7);

    QueueAvailabilityCalculator calculator = new QueueAvailabilityCalculator();

    boolean isFull = calculator.isTargetFull(type);

    assertFalse(isFull);
  }

  @Test
  void isServerFullReturnsTrueWhenTargetFull() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    QueueType type = config.QUEUE_TYPES[0];
    type.setReservedSlots(5);
    type.getPlayersWithTypeInTarget().set(5);

    QueueAvailabilityCalculator calculator = new QueueAvailabilityCalculator();

    boolean isFull = calculator.isServerFull(type);

    assertTrue(isFull);
  }

  @Test
  void isServerFullReturnsTrueWhenHasQueuedPlayers() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(10);
    QueueType type = config.QUEUE_TYPES[0];
    type.setReservedSlots(10);
    type.getPlayersWithTypeInTarget().set(0);
    type.getQueueMap().put(java.util.UUID.randomUUID(), new QueueType.QueuedPlayer("target", QueueReason.SERVER_FULL));

    QueueAvailabilityCalculator calculator = new QueueAvailabilityCalculator();

    boolean isFull = calculator.isServerFull(type);

    assertTrue(isFull);
  }

  @Test
  void isServerFullReturnsFalseWhenNotFullAndNoQueue() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(10);
    QueueType type = config.QUEUE_TYPES[0];
    type.setReservedSlots(10);
    type.getPlayersWithTypeInTarget().set(5);

    QueueAvailabilityCalculator calculator = new QueueAvailabilityCalculator();

    boolean isFull = calculator.isServerFull(type);

    assertFalse(isFull);
  }
}