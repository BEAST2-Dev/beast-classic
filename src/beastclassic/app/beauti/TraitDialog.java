package beastclassic.app.beauti;

import javax.swing.JDialog;

import beastfx.app.beauti.ThemeProvider;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.util.FXUtils;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

public class TraitDialog extends DialogPane {
	
	private TextField txtTraitname;
	ComboBox comboBox;
	String tree;
	public String getTree() {
		return tree;
	}

	public String getName() {
		return name;
	}

	String name;
	
	public TraitDialog(BeautiDoc doc, List<String> trees) {
		VBox box  = FXUtils.newVBox();
		
		HBox traitNameBox = FXUtils.newHBox(); 
		Label lblTraitName = new Label("Trait name");
		lblTraitName.setMinWidth(100);
		traitNameBox.getChildren().add(lblTraitName);
		
		txtTraitname = new TextField();
		txtTraitname.setId("traitname");
		txtTraitname.setText("newTrait");
		traitNameBox.getChildren().add(txtTraitname);
		txtTraitname.setMinWidth(200);
		box.getChildren().add(traitNameBox);

		HBox treeBox = FXUtils.newHBox(); 
		Label lblTree = new Label("Tree");
		lblTree.setMinWidth(100);
		treeBox.getChildren().add(lblTree);
		
		comboBox = new ComboBox();
		comboBox.getItems().addAll(trees.toArray());
		comboBox.getSelectionModel().select(0);
		comboBox.setMinWidth(200);
		treeBox.getChildren().add(comboBox);
		box.getChildren().add(treeBox);
		getChildren().add(box);
	}

	public boolean showDialog(String title) {
		Dialog dlg = new Dialog();
		dlg.setDialogPane(this);
		getButtonTypes().add(ButtonType.CANCEL);
		getButtonTypes().add(ButtonType.OK);
    	dlg.setTitle("Specify trait name and tree");
    	setMinHeight(260);
    	ThemeProvider.loadStyleSheet(dlg.getDialogPane().getScene());

    	Optional<ButtonType> result = dlg.showAndWait();
        if (!result.get().toString().contains("OK")) {
            return false;
        }
        
        tree = (String) comboBox.getSelectionModel().getSelectedItem();
        name = txtTraitname.getText().trim();
        return true;
	}
}
