package test.beast.app.beauti;

import static org.fest.assertions.Assertions.assertThat;

import java.awt.Component;
import java.io.File;
import java.util.Arrays;

import javax.swing.JOptionPane;

import org.fest.swing.core.GenericTypeMatcher;
import org.fest.swing.data.Index;
import org.fest.swing.fixture.JComboBoxFixture;
import org.fest.swing.fixture.JOptionPaneFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.fest.swing.fixture.JTableFixture;
import org.fest.swing.image.ScreenshotTaker;
import org.junit.Test;

public class BeautiContinuousPhylogeographyTest extends BeautiBase {

	JOptionPaneFixture dialog;

	@Test
	public void continuousPhylogeographyTestTutorial() throws Exception {
		// if (true) {return;}
		final String BASE_DIR = new File(".").getAbsolutePath() + "/doc/tutorial/phylogeography_continuous/figures";
		final String PREFIX = BASE_DIR + "/BEAUti_";
		try {
			long t0 = System.currentTimeMillis();

			beauti.frame.setSize(1024, 640);
			ScreenshotTaker screenshotTaker = new ScreenshotTaker();
			for (File file : new File(BASE_DIR).listFiles()) {
				if (file.getAbsolutePath().startsWith(PREFIX) && file.getName().endsWith(".png")) {
					file.delete();
				}
			}

			// 0. Load primate-mtDNA.nex
			warning("0. Load RABV.nex");
			importAlignment("doc/tutorial/phylogeography_continuous/data/", new File("RacRABV.nex"));

			JTabbedPaneFixture f = beautiFrame.tabbedPane();
			f.requireVisible();
			f.requireTitle("Partitions", Index.atIndex(0));
			String[] titles = f.tabTitles();
			assertArrayEquals(titles, "[Partitions, Tip Dates, Site Model, Clock Model, Priors, MCMC]");
			System.err.println(Arrays.toString(titles));
			f = f.selectTab("Partitions");

			
			
			// check table
			JTableFixture t = beautiFrame.table();
			printTableContents(t);
			checkTableContents(t, "[RacRABV, RacRABV, 47, 2811, nucleotide, RacRABV, RacRABV, RacRABV]");

			assertThat(f).isNotNull();
			printBeautiState(f);
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "DataPartitions.png");

			// 1. set up tip dates
			warning("1. Set up tip dates");
			f = f.selectTab("Tip Dates");
			warning("1. Seting up tip dates");
			beautiFrame.checkBox().click();
			beautiFrame.button("Guess").click();
			dialog = new JOptionPaneFixture(robot());

			dialog.radioButton("split on character").click();
			dialog.comboBox("splitCombo").selectItem("2");
			dialog.checkBox("Add fixed value").click();
			dialog.checkBox("Unless less than").click();
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "dates.png");
			dialog.okButton().click();
			printBeautiState(f);
			

			// 2. Set the site model to HKY (empirical)
			warning("2. Set the site model to HKY (empirical)");
			f.selectTab("Site Model");
			JComboBoxFixture substModel = beautiFrame.comboBox();
			substModel.selectItem("HKY");
			beautiFrame.comboBox("frequencies").selectItem("Empirical");
			printBeautiState(f);
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "sitemodel.png");
			
			// 3. Setup clock
			warning("3. Use strict clock");
			f.selectTab("Clock Model");
			printBeautiState(f);
			
			// 4. Set tree prior
			warning("4. Change tree prior to Coalescent with constant pop size");
			f.selectTab("Priors");
			beautiFrame.comboBox("TreeDistribution").selectItem("Coalescent Constant Population");
			printBeautiState(f);
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "priors.png");

			assertStateEquals("Tree.t:RacRABV", "clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV");
			assertOperatorsEqual("treeScaler.t:RacRABV", "treeRootScaler.t:RacRABV", "UniformOperator.t:RacRABV",
					"SubtreeSlide.t:RacRABV", "narrow.t:RacRABV", "wide.t:RacRABV", "WilsonBalding.t:RacRABV",
					"StrictClockRateScaler.c:RacRABV", "strictClockUpDownOperator.c:RacRABV", "KappaScaler.s:RacRABV",
					"PopSizeScaler.t:RacRABV");
			assertPriorsEqual("CoalescentConstant.t:RacRABV", "ClockPrior.c:RacRABV", "KappaPrior.s:RacRABV",
					"PopSizePrior.t:RacRABV");
			assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RacRABV", "TreeHeight.t:RacRABV",
					"clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV", "CoalescentConstant.t:RacRABV");

			// 5. Set up location trait
			warning("5. Set up location trait");
			f.selectTab("Partitions");
			beautiFrame.button("+").click();

			dialog = new JOptionPaneFixture(robot());
			dialog.comboBox("OptionPane.comboBox").selectItem("Add Continuous Geography");
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "geography1.png");
			dialog.okButton().click();

			dialog = new JOptionPaneFixture(robot());
			dialog.textBox("traitname").setText("location");
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "geography2.png");
			dialog.okButton().click();

			dialog = new JOptionPaneFixture(robot());
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "geography3.png");
			dialog.button("Guess latitude").click();

			GenericTypeMatcher<JOptionPane> matcher = new GenericTypeMatcher<JOptionPane>(JOptionPane.class) {
			protected boolean isMatching(JOptionPane optionPane) {
				Component o = (Component) optionPane.getMessage();
				while (o != null) {
					System.err.print(">" + o.getName() + "< ");
					if (o.getName() != null) {
						if (o.getName().equals("GuessTaxonSets")) {
							System.err.println("true");
							return o.isShowing();
						}
					}
					o = o.getParent();
				}
				System.err.println();
				return false;
			}
			};
			Component c = robot().finder().find(matcher);
			JOptionPaneFixture optionPane = new JOptionPaneFixture(robot(), (JOptionPane) c);//JOptionPaneFinder.findOptionPane(matcher).using(robot());

			// DialogFixture dialog2 =
			// WindowFinder.findDialog("GuessTaxonSets").using(robot());
			optionPane.radioButton("split on character").click();
			optionPane.comboBox("splitCombo").selectItem("3");
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "geography4.png");
			// JButton okButton =
			// dialog.robot.finder().find(JButtonMatcher.withText("OK").andShowing());
			// new JButtonFixture(dialog.robot, okButton).click();
			optionPane.okButton().click();

			dialog.button("Guess longitude").click();

			// dialog2 =
			// WindowFinder.findDialog("GuessTaxonSets").using(robot());
			//optionPane = JOptionPaneFinder.findOptionPane(matcher).using(robot());
			c = robot().finder().find(matcher);
			optionPane = new JOptionPaneFixture(robot(), (JOptionPane) c);
			optionPane.radioButton("split on character").click();
			optionPane.comboBox("splitCombo").selectItem("4");
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "geography5.png");
			// okButton =
			// dialog.robot.finder().find(JButtonMatcher.withText("OK").andShowing());
			// new JButtonFixture(dialog.robot, okButton).click();
			optionPane.okButton().click();

			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "geography6.png");
			dialog.button("OptionPane.button").click();
			printBeautiState(f);
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "DataPartitions2.png");

			assertStateEquals("Tree.t:RacRABV", "clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV",
					"clockRate.c:location", "precisionMatrix.s:location");
			assertOperatorsEqual("treeScaler.t:RacRABV", "treeRootScaler.t:RacRABV", "UniformOperator.t:RacRABV",
					"SubtreeSlide.t:RacRABV", "narrow.t:RacRABV", "wide.t:RacRABV", "WilsonBalding.t:RacRABV",
					"StrictClockRateScaler.c:RacRABV", "strictClockUpDownOperator.c:RacRABV", "KappaScaler.s:RacRABV",
					"PopSizeScaler.t:RacRABV", "StrictClockRateScaler.c:location",
					"strictClockUpDownOperator.c:location");
			assertPriorsEqual("CoalescentConstant.t:RacRABV", "ClockPrior.c:RacRABV", "KappaPrior.s:RacRABV",
					"PopSizePrior.t:RacRABV", "ClockPrior.c:location");
			assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RacRABV", "TreeHeight.t:RacRABV",
					"clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV", "CoalescentConstant.t:RacRABV",
					"clockRate.c:location", "precisionMatrix.s:location");

			// 6. View clock
			warning("6. View clock models");
			f.selectTab("Clock Model");
			printBeautiState(f);
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "clockmodel2.png");

			assertStateEquals("Tree.t:RacRABV", "clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV",
					"clockRate.c:location", "precisionMatrix.s:location");
			assertOperatorsEqual("treeScaler.t:RacRABV", "treeRootScaler.t:RacRABV", "UniformOperator.t:RacRABV",
					"SubtreeSlide.t:RacRABV", "narrow.t:RacRABV", "wide.t:RacRABV", "WilsonBalding.t:RacRABV",
					"StrictClockRateScaler.c:RacRABV", "strictClockUpDownOperator.c:RacRABV", "KappaScaler.s:RacRABV",
					"PopSizeScaler.t:RacRABV", "StrictClockRateScaler.c:location",
					"strictClockUpDownOperator.c:location");
			assertPriorsEqual("CoalescentConstant.t:RacRABV", "ClockPrior.c:RacRABV", "KappaPrior.s:RacRABV",
					"PopSizePrior.t:RacRABV", "ClockPrior.c:location");
			assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RacRABV", "TreeHeight.t:RacRABV",
					"clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV", "CoalescentConstant.t:RacRABV",
					"clockRate.c:location", "precisionMatrix.s:location");

			// 7. View priors
			warning("7. View priors");
			f.selectTab("Priors");
			printBeautiState(f);
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "priors2.png");

			assertStateEquals("Tree.t:RacRABV", "clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV",
					"clockRate.c:location", "precisionMatrix.s:location");
			assertOperatorsEqual("treeScaler.t:RacRABV", "treeRootScaler.t:RacRABV", "UniformOperator.t:RacRABV",
					"SubtreeSlide.t:RacRABV", "narrow.t:RacRABV", "wide.t:RacRABV", "WilsonBalding.t:RacRABV",
					"StrictClockRateScaler.c:RacRABV", "strictClockUpDownOperator.c:RacRABV", "KappaScaler.s:RacRABV",
					"PopSizeScaler.t:RacRABV", "StrictClockRateScaler.c:location",
					"strictClockUpDownOperator.c:location");
			assertPriorsEqual("CoalescentConstant.t:RacRABV", "ClockPrior.c:RacRABV", "KappaPrior.s:RacRABV",
					"PopSizePrior.t:RacRABV", "ClockPrior.c:location");
			assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RacRABV", "TreeHeight.t:RacRABV",
					"clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV", "CoalescentConstant.t:RacRABV",
					"clockRate.c:location", "precisionMatrix.s:location");

			// 8. Set up MCMC
			warning("8. Set up MCMC parameters");
			f.selectTab("MCMC");
			beautiFrame.textBox("chainLength").setText("20000000");
			printBeautiState(f);
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "MCMC.png");

			assertStateEquals("Tree.t:RacRABV", "clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV",
					"clockRate.c:location", "precisionMatrix.s:location");
			assertOperatorsEqual("treeScaler.t:RacRABV", "treeRootScaler.t:RacRABV", "UniformOperator.t:RacRABV",
					"SubtreeSlide.t:RacRABV", "narrow.t:RacRABV", "wide.t:RacRABV", "WilsonBalding.t:RacRABV",
					"StrictClockRateScaler.c:RacRABV", "strictClockUpDownOperator.c:RacRABV", "KappaScaler.s:RacRABV",
					"PopSizeScaler.t:RacRABV", "StrictClockRateScaler.c:location",
					"strictClockUpDownOperator.c:location");
			assertPriorsEqual("CoalescentConstant.t:RacRABV", "ClockPrior.c:RacRABV", "ClockPrior.c:location",
					"KappaPrior.s:RacRABV", "PopSizePrior.t:RacRABV");
			assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RacRABV", "TreeHeight.t:RacRABV",
					"clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV", "CoalescentConstant.t:RacRABV",
					"clockRate.c:location", "precisionMatrix.s:location");

			// 9. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree
			warning("9. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree");
			File fout = new File(org.fest.util.Files.temporaryFolder() + "/RacRABV.xml");
			if (fout.exists()) {
				fout.delete();
			}
			makeSureXMLParses();

			long t1 = System.currentTimeMillis();
			System.err.println("total time: " + (t1 - t0) / 1000 + " seconds");

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}


	@Test
	public void realaxedContinuousPhylogeographyTestTutorial() throws Exception {
		// if (true) {return;}
		final String BASE_DIR = new File(".").getAbsolutePath() + "/doc/tutorial/phylogeography_continuous/figures";
		final String PREFIX = BASE_DIR + "/BEAUti_";
		try {
			long t0 = System.currentTimeMillis();

			beauti.frame.setSize(1024, 640);
			ScreenshotTaker screenshotTaker = new ScreenshotTaker();
			for (File file : new File(BASE_DIR).listFiles()) {
				if (file.getAbsolutePath().startsWith(PREFIX) && file.getName().endsWith(".png")) {
					file.delete();
				}
			}

			// 0. Load primate-mtDNA.nex
			warning("0. Load RABV.nex");
			importAlignment("doc/tutorial/phylogeography_continuous/data/", new File("RacRABV.nex"));

			JTabbedPaneFixture f = beautiFrame.tabbedPane();
			f.requireVisible();
			f.requireTitle("Partitions", Index.atIndex(0));
			String[] titles = f.tabTitles();
			assertArrayEquals(titles, "[Partitions, Tip Dates, Site Model, Clock Model, Priors, MCMC]");
			System.err.println(Arrays.toString(titles));
			f = f.selectTab("Partitions");

			
			
			// check table
			JTableFixture t = beautiFrame.table();
			printTableContents(t);
			checkTableContents(t, "[RacRABV, RacRABV, 47, 2811, nucleotide, RacRABV, RacRABV, RacRABV]");

			assertThat(f).isNotNull();
			printBeautiState(f);
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "DataPartitions.png");

			// 1. set up tip dates
			warning("1. Set up tip dates");
			f = f.selectTab("Tip Dates");
			warning("1. Seting up tip dates");
			beautiFrame.checkBox().click();
			beautiFrame.button("Guess").click();
			dialog = new JOptionPaneFixture(robot());

			dialog.radioButton("split on character").click();
			dialog.comboBox("splitCombo").selectItem("2");
			dialog.checkBox("Add fixed value").click();
			dialog.checkBox("Unless less than").click();
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "dates.png");
			dialog.okButton().click();
			printBeautiState(f);
			

			// 2. Set the site model to HKY (empirical)
			warning("2. Set the site model to HKY (empirical)");
			f.selectTab("Site Model");
			JComboBoxFixture substModel = beautiFrame.comboBox();
			substModel.selectItem("HKY");
			beautiFrame.comboBox("frequencies").selectItem("Empirical");
			printBeautiState(f);
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "sitemodel.png");
			
			// 3. Setup clock
			warning("3. Use strict clock");
			f.selectTab("Clock Model");
			printBeautiState(f);
			
			// 4. Set tree prior
			warning("4. Change tree prior to Coalescent with constant pop size");
			f.selectTab("Priors");
			beautiFrame.comboBox("TreeDistribution").selectItem("Coalescent Constant Population");
			printBeautiState(f);
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "priors.png");

			assertStateEquals("Tree.t:RacRABV", "clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV");
			assertOperatorsEqual("treeScaler.t:RacRABV", "treeRootScaler.t:RacRABV", "UniformOperator.t:RacRABV",
					"SubtreeSlide.t:RacRABV", "narrow.t:RacRABV", "wide.t:RacRABV", "WilsonBalding.t:RacRABV",
					"StrictClockRateScaler.c:RacRABV", "strictClockUpDownOperator.c:RacRABV", "KappaScaler.s:RacRABV",
					"PopSizeScaler.t:RacRABV");
			assertPriorsEqual("CoalescentConstant.t:RacRABV", "ClockPrior.c:RacRABV", "KappaPrior.s:RacRABV",
					"PopSizePrior.t:RacRABV");
			assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RacRABV", "TreeHeight.t:RacRABV",
					"clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV", "CoalescentConstant.t:RacRABV");

			// 5. Set up location trait
			warning("5. Set up location trait");
			f.selectTab("Partitions");
			beautiFrame.button("+").click();

			dialog = new JOptionPaneFixture(robot());
			dialog.comboBox("OptionPane.comboBox").selectItem("Add Continuous Geography");
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "geography1.png");
			dialog.okButton().click();

			dialog = new JOptionPaneFixture(robot());
			dialog.textBox("traitname").setText("location");
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "geography2.png");
			dialog.okButton().click();

			dialog = new JOptionPaneFixture(robot());
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "geography3.png");
			dialog.button("Guess latitude").click();

			GenericTypeMatcher<JOptionPane> matcher = new GenericTypeMatcher<JOptionPane>(JOptionPane.class) {
			protected boolean isMatching(JOptionPane optionPane) {
				Component o = (Component) optionPane.getMessage();
				while (o != null) {
					System.err.print(">" + o.getName() + "< ");
					if (o.getName() != null) {
						if (o.getName().equals("GuessTaxonSets")) {
							System.err.println("true");
							return o.isShowing();
						}
					}
					o = o.getParent();
				}
				System.err.println();
				return false;
			}
			};
			Component c = robot().finder().find(matcher);
			JOptionPaneFixture optionPane = new JOptionPaneFixture(robot(), (JOptionPane) c);//JOptionPaneFinder.findOptionPane(matcher).using(robot());

			// DialogFixture dialog2 =
			// WindowFinder.findDialog("GuessTaxonSets").using(robot());
			optionPane.radioButton("split on character").click();
			optionPane.comboBox("splitCombo").selectItem("3");
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "geography4.png");
			// JButton okButton =
			// dialog.robot.finder().find(JButtonMatcher.withText("OK").andShowing());
			// new JButtonFixture(dialog.robot, okButton).click();
			optionPane.okButton().click();

			dialog.button("Guess longitude").click();

			// dialog2 =
			// WindowFinder.findDialog("GuessTaxonSets").using(robot());
			//optionPane = JOptionPaneFinder.findOptionPane(matcher).using(robot());
			c = robot().finder().find(matcher);
			optionPane = new JOptionPaneFixture(robot(), (JOptionPane) c);
			optionPane.radioButton("split on character").click();
			optionPane.comboBox("splitCombo").selectItem("4");
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "geography5.png");
			// okButton =
			// dialog.robot.finder().find(JButtonMatcher.withText("OK").andShowing());
			// new JButtonFixture(dialog.robot, okButton).click();
			optionPane.okButton().click();

			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "geography6.png");
			dialog.button("OptionPane.button").click();
			printBeautiState(f);
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "DataPartitions2.png");

			assertStateEquals("Tree.t:RacRABV", "clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV",
					"clockRate.c:location", "precisionMatrix.s:location");
			assertOperatorsEqual("treeScaler.t:RacRABV", "treeRootScaler.t:RacRABV", "UniformOperator.t:RacRABV",
					"SubtreeSlide.t:RacRABV", "narrow.t:RacRABV", "wide.t:RacRABV", "WilsonBalding.t:RacRABV",
					"StrictClockRateScaler.c:RacRABV", "strictClockUpDownOperator.c:RacRABV", "KappaScaler.s:RacRABV",
					"PopSizeScaler.t:RacRABV", "StrictClockRateScaler.c:location",
					"strictClockUpDownOperator.c:location");
			assertPriorsEqual("CoalescentConstant.t:RacRABV", "ClockPrior.c:RacRABV", "KappaPrior.s:RacRABV",
					"PopSizePrior.t:RacRABV", "ClockPrior.c:location");
			assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RacRABV", "TreeHeight.t:RacRABV",
					"clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV", "CoalescentConstant.t:RacRABV",
					"clockRate.c:location", "precisionMatrix.s:location");

			// 6. View clock/set log normal RWW 
			warning("6. View clock models/set log normal RWW");
			f.selectTab("Clock Model");
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "clockmodel2.png");
			beautiFrame.list().selectItem(1);
			beautiFrame.comboBox().selectItem("Relaxed Clock Log Normal");
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "clockmodel3.png");

			printBeautiState(f);
			assertStateEquals("Tree.t:RacRABV", "clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV", "precisionMatrix.s:location", "ucldMean.c:location", "ucldStdev.c:location", "rateCategories.c:location");
			assertOperatorsEqual("treeScaler.t:RacRABV", "treeRootScaler.t:RacRABV", "UniformOperator.t:RacRABV", "SubtreeSlide.t:RacRABV", "narrow.t:RacRABV", "wide.t:RacRABV", "WilsonBalding.t:RacRABV", "StrictClockRateScaler.c:RacRABV", "strictClockUpDownOperator.c:RacRABV", "KappaScaler.s:RacRABV", "PopSizeScaler.t:RacRABV", "ucldMeanScaler.c:location", "ucldStdevScaler.c:location", "CategoriesRandomWalk.c:location", "CategoriesSwapOperator.c:location", "CategoriesUniform.c:location", "relaxedUpDownOperator.c:location");
			assertPriorsEqual("CoalescentConstant.t:RacRABV", "ClockPrior.c:RacRABV", "KappaPrior.s:RacRABV", "PopSizePrior.t:RacRABV", "ucldStdevPrior.c:location", "MeanRatePrior.c:location");
			assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RacRABV", "TreeHeight.t:RacRABV", "clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV", "CoalescentConstant.t:RacRABV", "precisionMatrix.s:location", "ucldMean.c:location", "ucldStdev.c:location", "rate.c:location");

			// 7. View priors
			warning("7. View priors");
			f.selectTab("Priors");
			printBeautiState(f);
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "priors2.png");
			assertStateEquals("Tree.t:RacRABV", "clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV", "precisionMatrix.s:location", "ucldMean.c:location", "ucldStdev.c:location", "rateCategories.c:location");
			assertOperatorsEqual("treeScaler.t:RacRABV", "treeRootScaler.t:RacRABV", "UniformOperator.t:RacRABV", "SubtreeSlide.t:RacRABV", "narrow.t:RacRABV", "wide.t:RacRABV", "WilsonBalding.t:RacRABV", "StrictClockRateScaler.c:RacRABV", "strictClockUpDownOperator.c:RacRABV", "KappaScaler.s:RacRABV", "PopSizeScaler.t:RacRABV", "ucldMeanScaler.c:location", "ucldStdevScaler.c:location", "CategoriesRandomWalk.c:location", "CategoriesSwapOperator.c:location", "CategoriesUniform.c:location", "relaxedUpDownOperator.c:location");
			assertPriorsEqual("CoalescentConstant.t:RacRABV", "ClockPrior.c:RacRABV", "KappaPrior.s:RacRABV", "PopSizePrior.t:RacRABV", "ucldStdevPrior.c:location", "MeanRatePrior.c:location");
			assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RacRABV", "TreeHeight.t:RacRABV", "clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV", "CoalescentConstant.t:RacRABV", "precisionMatrix.s:location", "ucldMean.c:location", "ucldStdev.c:location", "rate.c:location");

			// 8. Set up MCMC
			warning("8. Set up MCMC parameters");
			f.selectTab("MCMC");
			beautiFrame.textBox("chainLength").setText("5000000");
			printBeautiState(f);
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "MCMC.png");
			assertStateEquals("Tree.t:RacRABV", "clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV", "precisionMatrix.s:location", "ucldMean.c:location", "ucldStdev.c:location", "rateCategories.c:location");
			assertOperatorsEqual("treeScaler.t:RacRABV", "treeRootScaler.t:RacRABV", "UniformOperator.t:RacRABV", "SubtreeSlide.t:RacRABV", "narrow.t:RacRABV", "wide.t:RacRABV", "WilsonBalding.t:RacRABV", "StrictClockRateScaler.c:RacRABV", "strictClockUpDownOperator.c:RacRABV", "KappaScaler.s:RacRABV", "PopSizeScaler.t:RacRABV", "ucldMeanScaler.c:location", "ucldStdevScaler.c:location", "CategoriesRandomWalk.c:location", "CategoriesSwapOperator.c:location", "CategoriesUniform.c:location", "relaxedUpDownOperator.c:location");
			assertPriorsEqual("CoalescentConstant.t:RacRABV", "ClockPrior.c:RacRABV", "KappaPrior.s:RacRABV", "PopSizePrior.t:RacRABV", "MeanRatePrior.c:location", "ucldStdevPrior.c:location");
			assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RacRABV", "TreeHeight.t:RacRABV", "clockRate.c:RacRABV", "kappa.s:RacRABV", "popSize.t:RacRABV", "CoalescentConstant.t:RacRABV", "precisionMatrix.s:location", "ucldMean.c:location", "ucldStdev.c:location", "rate.c:location");

			
			// 9. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree
			warning("9. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree");
			File fout = new File(org.fest.util.Files.temporaryFolder() + "/RacRABV.xml");
			if (fout.exists()) {
				fout.delete();
			}
			makeSureXMLParses();

			long t1 = System.currentTimeMillis();
			System.err.println("total time: " + (t1 - t0) / 1000 + " seconds");

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

}
