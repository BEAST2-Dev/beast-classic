package beast.app.beauti;

import javax.swing.JDialog;

import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import javax.swing.JComboBox;
import javax.swing.border.EmptyBorder;

import beast.evolution.tree.Tree;

import java.awt.Insets;
import java.util.List;

public class TraitDialog extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private JTextField txtTraitname;
	JComboBox comboBox;
	String tree;
	String name;
	
	public TraitDialog(BeautiDoc doc, List<String> trees) {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[]{1.0, 0.0};
		setLayout(gridBagLayout);
		
		JLabel lblTraitName = new JLabel("Trait name");
		GridBagConstraints gbc_lblTraitName = new GridBagConstraints();
		gbc_lblTraitName.anchor = GridBagConstraints.EAST;
		gbc_lblTraitName.insets = new Insets(0, 0, 5, 5);
		gbc_lblTraitName.gridx = 0;
		gbc_lblTraitName.gridy = 0;
		add(lblTraitName, gbc_lblTraitName);
		
		txtTraitname = new JTextField();
		txtTraitname.setText("newTrait");
		GridBagConstraints gbc_txtTraitname = new GridBagConstraints();
		gbc_txtTraitname.insets = new Insets(0, 0, 5, 0);
		gbc_txtTraitname.gridx = 1;
		gbc_txtTraitname.gridy = 0;
		add(txtTraitname, gbc_txtTraitname);
		txtTraitname.setColumns(10);
		
		JLabel lblTree = new JLabel("Tree");
		GridBagConstraints gbc_lblTree = new GridBagConstraints();
		gbc_lblTree.insets = new Insets(0, 0, 0, 5);
		gbc_lblTree.anchor = GridBagConstraints.EAST;
		gbc_lblTree.gridx = 0;
		gbc_lblTree.gridy = 1;
		add(lblTree, gbc_lblTree);
		
		comboBox = new JComboBox(trees.toArray());
		GridBagConstraints gbc_comboBox = new GridBagConstraints();
		gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBox.gridx = 1;
		gbc_comboBox.gridy = 1;
		add(comboBox, gbc_comboBox);
	}

	public boolean showDialog(String title) {
        JOptionPane optionPane = new JOptionPane(this, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION, null, new String[]{"Cancel", "OK"}, "OK");
        optionPane.setBorder(new EmptyBorder(12, 12, 12, 12));

        final JDialog dialog = optionPane.createDialog(null, title);
    	dialog.setName("TraitDialog");
        // dialog.setResizable(true);
        dialog.pack();

        dialog.setVisible(true);
        if (!optionPane.getValue().equals("OK")) {
            return false;
        }
        
        tree = (String) comboBox.getSelectedItem();
        name = txtTraitname.getText().trim();
        return true;
	}
}
