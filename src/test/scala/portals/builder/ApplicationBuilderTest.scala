package portals

import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.Assert._
import org.junit.Ignore
import org.junit.Test

import portals.test.*

@RunWith(classOf[JUnit4])
class ApplicationBuilderTest:

  @Ignore
  @Test
  def testExternalCycle(): Unit =
    import portals.DSL.*

    val testData = List(List(8))

    val tester = new TestUtils.Tester[Int]()

    val builder = ApplicationBuilders.application("application")

    val sequencer = builder.sequencers.random[Int]()

    val workflow = builder
      .workflows[Int, Int]("wf")
      .source[Int](sequencer.stream)
      .flatMap[Int] { ctx ?=> x =>
        if (x > 0) List(x - 1)
        else List.empty
      }
      .task(tester.task)
      .sink()
      .freeze()

    val _ = builder.connections.connect(workflow.stream, sequencer)

    val generator = builder.generators.fromList[Int](List(8))
    val _ = builder.connections.connect(generator.stream, sequencer)

    val application = builder.build()

    val system = Systems.syncLocal()

    system.launch(application)

    system.stepAll()
    system.shutdown()

    // the output counts down atoms (List with single elements) from 7 to 0 and stops
    assertEquals(Some(List(7)), tester.receiveAtom())
    assertEquals(Some(List(6)), tester.receiveAtom())
    assertEquals(Some(List(5)), tester.receiveAtom())
    assertEquals(Some(List(4)), tester.receiveAtom())
    assertEquals(Some(List(3)), tester.receiveAtom())
    assertEquals(Some(List(2)), tester.receiveAtom())
    assertEquals(Some(List(1)), tester.receiveAtom())
    assertEquals(Some(List(0)), tester.receiveAtom())
    assertNotEquals(Some(List(-1)), tester.receiveAtom())

  @Ignore
  @Test
  def testChainOfWorkflows(): Unit =
    import portals.DSL.*

    val tester = new TestUtils.Tester[Int]()

    val builder = ApplicationBuilders.application("app")

    // 0, Atom, 1, Atom, ..., 4, Atom, Seal
    val input = List.range(0, 5).grouped(1).toList
    val generator = builder.generators.fromListOfLists(input)

    def workflowFactory(name: String, stream: AtomicStreamRef[Int]): Workflow[Int, Int] =
      builder
        .workflows[Int, Int](name)
        .source[Int](stream)
        .map(_ + 1)
        .sink()
        .freeze()

    // chain length 4
    val wf1 = workflowFactory("wf1", generator.stream)
    val wf2 = workflowFactory("wf2", wf1.stream)
    val wf3 = workflowFactory("wf3", wf2.stream)
    val wf4 = workflowFactory("wf4", wf3.stream)
    val twf = tester.workflow(wf4.stream, builder)

    val application = builder.build()

    val system = Systems.syncLocal()
    system.launch(application)

    system.stepAll()
    system.shutdown()

    input.foreach { list =>
      list.foreach { message =>
        // receive message + length of chain (4)
        tester.receiveAssert(message + 4)
      }
    }

  @Test
  def testChainOfTasks(): Unit =
    import portals.DSL.*

    val tester = new TestUtils.Tester[Int]()

    val builder = ApplicationBuilders
      .application("app")

    // 0, Atom, 1, Atom, ..., 4, Atom, Seal
    val input = List.range(0, 5).grouped(1).toList
    val generator = builder.generators.fromListOfLists(input)

    val workflow = builder
      .workflows[Int, Int]("workflow")
      .source[Int](generator.stream)
      // chain length 4
      .map(_ + 1)
      .map(_ + 1)
      .map(_ + 1)
      .map(_ + 1)
      .task(tester.task)
      // .logger()
      .sink()
      .freeze()

    val application = builder.build()

    val system = Systems.syncLocal()
    system.launch(application)

    system.stepAll()
    system.shutdown()

    input.foreach { list =>
      list.foreach { message =>
        // receive message + length of chain (4)
        tester.receiveAssert(message + 4)
      }
    }

  @Test
  def basicAtomsTest(): Unit =
    import portals.DSL.*

    val testData = List.range(0, 1024).grouped(128).toList

    // simple workflow that forwards any input to the output
    val flow = TestUtils.flowBuilder {
      _.identity()
    }

    val tester = TestUtils.executeWorkflow(flow, testData)

    testData.foreach { atom =>
      assertEquals(Some(atom), tester.receiveAtom())
    }

  @Test
  def basicAtomTest(): Unit =
    import portals.DSL.*

    val testData = List(List(1), List(2, 3), List(4, 5, 6))

    // simple workflow that forwards any input to the output
    val flow = TestUtils.flowBuilder {
      _.identity()
    }

    val tester = TestUtils.executeWorkflow(flow, testData)

    assertEquals(
      List(
        tester.Event(1),
        tester.Atom,
        tester.Event(2),
        tester.Event(3),
        tester.Atom,
        tester.Event(4),
        tester.Event(5),
        tester.Event(6),
        tester.Atom
      ),
      tester.receiveAllWrapped()
    )

  @Test
  def testDiamond(): Unit =
    import portals.DSL.*

    val testData = List.range(0, 256).grouped(128).toList

    // simple workflow that forwards any input to the output
    val flow = TestUtils.flowBuilder { flow =>
      val flow1 = flow.identity()
      val flow2 = flow.identity()
      flow1.union(flow2)
    }

    val tester = TestUtils.executeWorkflow(flow, testData)

    val firstAtom = testData(0)
    tester.receiveAtom().get.foreach { event =>
      assertTrue(firstAtom.contains(event))
    }

    val secondAtom = testData(1)
    tester.receiveAtom().get.foreach { event =>
      assertTrue(secondAtom.contains(event))
    }

  @Ignore
  @Test
  def testDiamond2(): Unit =
    import portals.DSL.*

    val testData = List.range(0, 2).grouped(1).toList

    // simple workflow that forwards any input to the output
    val flow = TestUtils.flowBuilder { flow =>
      val flow1 = flow.identity()
      val flow2 = flow.identity()
      flow1.union(flow2)
    }

    val tester = TestUtils.executeWorkflow(flow, testData)

    assertEquals(
      List(
        tester.Event(0),
        tester.Event(0),
        tester.Atom,
        tester.Event(1),
        tester.Event(1),
        tester.Atom
      ),
      tester.receiveAllWrapped()
    )

  @Test
  def testDiamond3(): Unit =
    import portals.DSL.*

    val testData = List.range(0, 256).grouped(1).toList

    // simple workflow that forwards any input to the output
    val flow = TestUtils.flowBuilder { flow =>
      val flow1 = flow.identity()
      val flow2 = flow.identity()
      flow1.union(flow2)
    }

    val tester = TestUtils.executeWorkflow(flow, testData)

    testData.foreach { atom =>
      atom.foreach { event =>
        tester.receiveAssert(event)
        tester.receiveAssert(event)
      }
    }
