package test.beast.app.beauti;

import static org.fest.assertions.Assertions.assertThat;

import java.awt.Component;
import java.io.File;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.fest.swing.core.GenericTypeMatcher;
import org.fest.swing.core.matcher.JButtonMatcher;
import org.fest.swing.data.Index;
import org.fest.swing.finder.JOptionPaneFinder;
import org.fest.swing.finder.WindowFinder;
import org.fest.swing.fixture.DialogFixture;
import org.fest.swing.fixture.JButtonFixture;
import org.fest.swing.fixture.JCheckBoxFixture;
import org.fest.swing.fixture.JComboBoxFixture;
import org.fest.swing.fixture.JOptionPaneFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.fest.swing.fixture.JTableFixture;
import org.fest.swing.fixture.JTextComponentFixture;
import org.fest.swing.image.ScreenshotTaker;
import org.junit.Test;

import beast.app.beauti.GuessPatternDialog;

public class BeautiPhylogeographyTest extends BeautiBase {

	JOptionPaneFixture dialog;

	@Test
	public void discretePhylogeographyTestTutorial() throws Exception {
		try {
			final String BASE_DIR = new File(".").getAbsolutePath() + "/doc/tutorial/phylogeography_discrete/figures";
			final String PREFIX = BASE_DIR + "/BEAUti_";

			// try {
			long t0 = System.currentTimeMillis();

			beauti.frame.setSize(1024, 640);
			ScreenshotTaker screenshotTaker = new ScreenshotTaker();
			for (File file : new File(BASE_DIR).listFiles()) {
				if (file.getAbsolutePath().startsWith(PREFIX) && file.getName().endsWith(".png")) {
					System.err.println("Deleting " + file.getPath());
					file.delete();
				}
			}

			// 0. Load primate-mtDNA.nex
			warning("0. Load H5N1.nex");
			importAlignment("doc/tutorial/phylogeography_discrete/data/", new File("H5N1.nex"));

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
			checkTableContents(t, "[H5N1, H5N1, 43, 1698, nucleotide, H5N1, H5N1, H5N1]");

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
			dialog.comboBox("delimiterCombo").selectItem("after last");
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "dates.png");

			dialog.okButton().click();
			printBeautiState(f);

			// 2. Set the site model to HKY+G4 (empirical)
			warning("2. Set the site model to HKY (empirical)");
			f.selectTab("Site Model");
			JComboBoxFixture substModel = beautiFrame.comboBox();
			substModel.selectItem("HKY");
			beautiFrame.comboBox("frequencies").selectItem("Empirical");

			JTextComponentFixture categoryCount = beautiFrame.textBox("gammaCategoryCount");
			categoryCount.setText("4");

			JCheckBoxFixture shapeIsEstimated = beautiFrame.checkBox("shape.isEstimated");
			shapeIsEstimated.check();
			printBeautiState(f);
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "sitemodel.png");

			// 3. Fix clock rate
			warning("3. Fix clock rate");
			f.selectTab("Clock Model");
			beautiFrame.menuItemWithPath("Mode", "Automatic set clock rate").click();
			beautiFrame.textBox("clock.rate").setText("0.004");
			beautiFrame.checkBox("clock.rate.isEstimated").click();
			printBeautiState(f);

			beautiFrame.menuItemWithPath("Mode").click();
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "clockmodel.png");

			// 4. Set tree prior
			warning("4. Change tree prior to Coalescent with constant pop size");
			f.selectTab("Priors");
			// tab seems to get stuck some times, so wait a second and press
			// again.
			Thread.sleep(1000);
			f.selectTab("Priors");
			beautiFrame.comboBox("TreeDistribution").selectItem("Coalescent Constant Population");
			printBeautiState(f);
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "priors.png");

			// 5. Set up discrete trait
			warning("5. Set up discrete trait");
			f.selectTab("Partitions");
			beautiFrame.button("+").click();

			dialog = new JOptionPaneFixture(robot());
			dialog.component().setLocation(0, 0);
			dialog.comboBox("OptionPane.comboBox").selectItem("Add Discrete Trait");
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "trait.png");
			dialog.okButton().click();

			dialog = new JOptionPaneFixture(robot());
			dialog.textBox("traitname").setText("location");
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "trait2.png");
			dialog.okButton().click();

			dialog = new JOptionPaneFixture(robot());
			dialog.button("guess").click();

			DialogFixture dialog2 = WindowFinder.findDialog("GuessTaxonSets").using(robot());
			dialog2.radioButton("split on character").click();
			dialog2.comboBox("splitCombo").selectItem("3");
			JButton okButton = dialog.robot.finder().find(JButtonMatcher.withText("OK").andShowing());
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "trait3.png");
			new JButtonFixture(dialog.robot, okButton).click();
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "trait4.png");

			dialog.button("OptionPane.button").click();
			printBeautiState(f);
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "DataPartitions2.png");

			// 6. View clock
			warning("6. View clock models");
			f.selectTab("Clock Model");
			beautiFrame.list().selectItem("location");
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "clockmodel2.png");

			// 7. View priors
			warning("7. View priors");
			f.selectTab("Priors");
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "priors2.png");

			// 8. Set up MCMC
			warning("8. Set up MCMC parameters");
			f.selectTab("MCMC");
			beautiFrame.textBox("chainLength").setText("3000000");
			screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "MCMC.png");

			// 9. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree
			warning("9. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree");
			File fout = new File(org.fest.util.Files.temporaryFolder() + "/H5N1.xml");
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
//			e.printStackTrace();
			throw e;
		}
	}

}