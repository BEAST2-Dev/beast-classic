package beastclassic.app.beauti;




import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.GuessPatternDialog;
import beastfx.app.inputeditor.ListInputEditor;
import beastfx.app.inputeditor.SmallLabel;
import beastfx.app.util.Alert;
import beastfx.app.util.FXUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import beastclassic.evolution.tree.TreeTraitMap;
import beastclassic.app.beauti.TraitInputEditor.LocationMap;
import beastclassic.continuous.SampledMultivariateTraitLikelihood;
import beastclassic.evolution.alignment.AlignmentFromTraitMap;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import beast.base.core.BEASTInterface;
import beast.base.core.BEASTObject;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeInterface;



public class LocationInputEditor extends ListInputEditor {
	public LocationInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public Class<?> baseType() {
		return SampledMultivariateTraitLikelihood.class;
	}

	SampledMultivariateTraitLikelihood likelihood;
	TreeInterface tree;
    TreeTraitMap traitSet;
    //JTextField traitEntry;
    //ComboBox relativeToComboBox;
    String [] sTaxa;
    // Object[][] tableData;
    
    
    
    public class LocationMap {
		String taxon;
    	Double latitude;
    	Double longitude;

    	LocationMap(String taxon, Double latitude, Double longitude) {
    		this.taxon = taxon;
    		this.latitude = latitude;
    		this.longitude = longitude;
    	}
    	
    	public String getTaxon() {
			return taxon;
		}
		public void setTaxon(String taxon) {
			this.taxon = taxon;
		}
		public Double getLongitude() {
			return longitude;
		}
		public void setLongitude(Double longitude) {
			this.longitude = longitude;
		}
		public Double getLatitude() {
			return latitude;
		}
		public void setLatitude(Double latitude) {
			this.latitude = latitude;
		}
    }
    TableView<LocationMap> table;
    ObservableList<LocationMap> taxonMapping;

    //UserDataType dataType;

    //String m_sPattern = ".*(\\d\\d\\d\\d).*";
    String m_sPattern = ".*_(..).*";

	@Override
	public void init(Input<?> input, BEASTInterface plugin, int itemNr,	ExpandOption bExpandOption, boolean bAddButtons) {
        m_bAddButtons = bAddButtons;
        m_input = input;
        m_beastObject = plugin;
        this.itemNr = itemNr;
        m_bAddButtons = bAddButtons;
		this.itemNr = itemNr;
		if (itemNr >= 0) {
			likelihood = (SampledMultivariateTraitLikelihood) ((ArrayList<?>)input.get()).get(itemNr);
		} else {
			likelihood = (SampledMultivariateTraitLikelihood) ((ArrayList<?>)input.get()).get(0);
		}
	}

	public void initPanel(SampledMultivariateTraitLikelihood likelihood_) {
		likelihood = likelihood_;
		m_beastObject = likelihood.dataInput.get();
		try {
			m_input = m_beastObject.getInput("traitSet");
		}catch (Exception e) {
			// TODO: handle exception
		}
		
        tree = likelihood.treeInput.get();
        if (tree != null) {
        	Alignment data = likelihood.dataInput.get();
        	if (!(data instanceof AlignmentFromTraitMap)) {
        		return;
        	}
    		AlignmentFromTraitMap traitData = (AlignmentFromTraitMap) data;
            m_input = traitData.traitInput;
            m_beastObject = traitData;
            traitSet = traitData.traitInput.get();
            
            VBox box = new VBox();

            if (traitSet != null) {
                box.getChildren().add(createButtonBox());
                box.getChildren().add(createListBox());
                box.getChildren().add(createButtonBox2());
            }
            getChildren().add(box);
            validateInput();
            // synchronise with table, useful when taxa have been deleted
            convertTableDataToTrait();
        }
    } // init



    private ScrollPane createListBox() {
    	try {
    		//traitSet.treeInput.get().getTaxaNames();
        	TreeInterface tree = traitSet.treeInput.get();
        	TaxonSet taxa = tree.getTaxonset();
        	taxa.initAndValidate();
    	} catch (Exception e) {
			// TODO: handle exception
		}
        sTaxa = traitSet.treeInput.get().getTaxonset().asStringList().toArray(new String[0]);
        String[] columnData = new String[]{"Name", "Latitude", "Longitude"};
        //tableData = new Object[sTaxa.length][3];
        
        taxonMapping = FXCollections.observableArrayList();
        for (String s : sTaxa) {
        	taxonMapping.add(new LocationMap(s, Double.NaN, Double.NaN));
        }
        convertTraitToTableData();

        // set up table.
        // special features: background shading of rows
        // custom editor allowing only Date column to be edited.
        table = new TableView<>();        
        table.setPrefWidth(1024);
        table.setEditable(true);
        table.setItems(taxonMapping);

        TableColumn<LocationMap, String> col1 = new TableColumn<>("Taxon");
        col1.setPrefWidth(700);
        col1.setEditable(false);
        col1.setCellValueFactory(
        	    new PropertyValueFactory<LocationMap,String>("Taxon")
        	);
        table.getColumns().add(col1);        

        TableColumn<LocationMap, Double> col2 = new TableColumn<>("Latitude");
        col2.setPrefWidth(100);
        col2.setEditable(true);
        col2.setCellValueFactory(
        	    new PropertyValueFactory<LocationMap,Double>("Latitude")
        	);
        col2.setCellFactory(new Callback<TableColumn<LocationMap, Double>,TableCell<LocationMap, Double>>() {
			@Override
			public TableCell<LocationMap, Double> call(TableColumn<LocationMap, Double> param) {
				StringConverter<Double> sc = new DoubleStringConverter();
				TextFieldTableCell<LocationMap, Double> tc = new TextFieldTableCell<LocationMap, Double>(sc) {
                    @Override
                    public void updateItem(Double item, boolean empty) {
                      super.updateItem(item, empty);
                      if (!isEmpty()) {
                        if (!Double.isNaN(item)) { 
                        	this.setTextFill(Color.GREEN);
                        } else { 
                        	this.setTextFill(Color.RED);
                        }
                        setText(String.valueOf(item));
                      }
                    }
                  };
                  tc.setConverter(sc);
                  return tc;
              }
		});
        col2.setOnEditCommit(
                new EventHandler<CellEditEvent<LocationMap, Double>>() {
  					@Override
  					public void handle(CellEditEvent<LocationMap, Double> event) {
  						Double newValue = event.getNewValue();
  						LocationMap location = event.getRowValue();
  						location.setLatitude(newValue);
  						convertTableDataToTrait();
  						validateInput();
  						table.refresh();
  					}
  				}                
            );

        table.getColumns().add(col2);
        
        TableColumn<LocationMap, Double> col3 = new TableColumn<>("Longitude");
        col3.setPrefWidth(100);
        col3.setEditable(true);
        col3.setCellValueFactory(
        	    new PropertyValueFactory<LocationMap,Double>("Longitude")
        	);
        col3.setCellFactory(new Callback<TableColumn<LocationMap, Double>,TableCell<LocationMap, Double>>() {
			@Override
			public TableCell<LocationMap, Double> call(TableColumn<LocationMap, Double> param) {
				StringConverter<Double> sc = new DoubleStringConverter();
				TextFieldTableCell<LocationMap, Double> tc = new TextFieldTableCell<LocationMap, Double>(sc) {
                    @Override
                    public void updateItem(Double item, boolean empty) {
                      super.updateItem(item, empty);
                      if (!isEmpty()) {
                        if (!Double.isNaN(item)) { 
                        	this.setTextFill(Color.GREEN);
                        } else { 
                        	this.setTextFill(Color.RED);
                        }
                        setText(String.valueOf(item));
                      }
                    }
                  };
                  tc.setConverter(sc);
                  return tc;
              }
		});
        col3.setOnEditCommit(
                new EventHandler<CellEditEvent<LocationMap, Double>>() {
  					@Override
  					public void handle(CellEditEvent<LocationMap, Double> event) {
  						Double newValue = event.getNewValue();
  						LocationMap location = event.getRowValue();
  						location.setLongitude(newValue);
  						convertTableDataToTrait();
  						validateInput();
  						table.refresh();
  					}
  				}                
            );
        table.getColumns().add(col3);   
        
//        table = new Table(tableData, columnData) {
//            private static final long serialVersionUID = 1L;
//
//            // method that induces table row shading
//            @Override
//            public Component prepareRenderer(TableCellRenderer renderer, int Index_row, int Index_col) {
//                Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
//                //even index, selected or not selected
//                if (isCellSelected(Index_row, Index_col)) {
//                    comp.setBackground(Color.lightGray);
//                } else if (Index_row % 2 == 0 && !isCellSelected(Index_row, Index_col)) {
//                    comp.setBackground(new Color(237, 243, 255));
//                } else {
//                    comp.setBackground(Color.white);
//                }
//                return comp;
//            }
//        };
//
//        // set up editor that makes sure only doubles are accepted as entry
//        // and only the Date column is editable.
//        table.setDefaultEditor(Object.class, new TableCellEditor() {
//            JTextField m_textField = new JTextField();
//            int m_iRow, m_iCol;
//
//            @Override
//            public boolean stopCellEditing() {
//                table.removeEditor();
//                String sText = m_textField.getText();
//                if (sText == "") {
//                	return false;
//                }
//                tableData[m_iRow][m_iCol] = sText;
//                convertTableDataToTrait();
//                convertTraitToTableData();
//                validateInput();
//                return true;
//            }
//
//            @Override
//            public boolean isCellEditable(EventObject anEvent) {
//                return table.getSelectedColumn() >= 1;
//            }
//
//
//            @Override
//            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int iRow, int iCol) {
//                if (!isSelected) {
//                    return null;
//                }
//                m_iRow = iRow;
//                m_iCol = iCol;
//                m_textField.setText((String) value);
//                return m_textField;
//            }
//
//            @Override
//            public boolean shouldSelectCell(EventObject anEvent) {
//                return false;
//            }
//
//            @Override
//            public void removeCellEditorListener(CellEditorListener l) {
//            }
//
//            @Override
//            public Object getCellEditorValue() {
//                return null;
//            }
//
//            @Override
//            public void cancelCellEditing() {
//            }
//
//            @Override
//            public void addCellEditorListener(CellEditorListener l) {
//            }
//
//        });
//        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
//        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//        table.setRowHeight(24);
//        table.getColumnModel().getColumn(0).setPreferredWidth(300);
//        table.getColumnModel().getColumn(1).setPreferredWidth(75);
//        table.getColumnModel().getColumn(2).setPreferredWidth(75);
        ScrollPane scrollPane = new ScrollPane(table);
        return scrollPane;
    } // createListBox

    /* synchronise table with data from traitSet Plugin */
    private void convertTraitToTableData() {
        for (int i = 0; i < taxonMapping.size(); i++) {
        	LocationMap location = taxonMapping.get(i);
        	location.taxon = sTaxa[i];
        	location.latitude = Double.NaN;
        	location.longitude = Double.NaN;
        }
        String trait = traitSet.value.get();
        if (trait == null || trait.trim().length() == 0) {
        	return;
        }
        String[] sTraits = trait.split(",");
        for (String sTrait : sTraits) {
            //sTrait = sTrait.replaceAll("\\s+", " ");
            String[] sStrs = sTrait.split("=");
            String value = null;
            if (sStrs.length != 2) {
            	value = "";
                //throw new Exception("could not parse trait: " + sTrait);
            } else {
            	value = sStrs[1].trim();
            }
            String sTaxonID = sStrs[0].trim();
            int iTaxon = indexOf(sTaxonID);
            if (iTaxon < 0) {
            	System.err.println(sTaxonID);
//                throw new Exception("Trait (" + sTaxonID + ") is not a known taxon. Spelling error perhaps?");
            } else {
            	LocationMap location = taxonMapping.get(iTaxon);
            	location.taxon = sTaxonID;
	            String [] sStrs2 = value.trim().split("\\s+");
	            if (sStrs2.length == 2) {
	            	location.latitude = parseDouble(sStrs2[0]);
	            	location.longitude = parseDouble(sStrs2[1]);
	            } else {
	            	if (Character.isSpace(sStrs[1].charAt(0))) {
	            		location.latitude = Double.NaN;
	            		location.longitude = parseDouble(sStrs2[0]);
	            	} else {
	            		location.latitude = parseDouble(sStrs2[0]);
	            		location.longitude = Double.NaN;
	            	}
	            }
            }
        }

        if (table != null) {
        	table.refresh();
        }
//        if (table != null) {
//            for (int i = 0; i < taxonMapping.size(); i++) {
//                table.setValueAt(tableData[i][1], i, 1);
//                table.setValueAt(tableData[i][2], i, 2);
//            }
//        }
    } // convertTraitToTableData

    private Double parseDouble(String string) {
		try {
			return Double.parseDouble(string);
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

	private int indexOf(String sTaxonID) {
		for (int i = 0; i < sTaxa.length; i++) {
			if (sTaxa[i].equals(sTaxonID)) {
				return i;
			}
		}
		return -1;
	}

	/**
     * synchronise traitSet Plugin with table data
     */
    void convertTableDataToTrait() {
        String sTrait = "";
        //Set<String> values = new HashSet<String>(); 
        for (int i = 0; i < taxonMapping.size(); i++) {
            sTrait += sTaxa[i] + "=" + taxonMapping.get(i).latitude + " " + taxonMapping.get(i).longitude;
            if (i < taxonMapping.size() - 1) {
                sTrait += ",\n";
            }
        }
        try {
            traitSet.value.setValue(sTrait, traitSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * create box with comboboxes for selection units and trait name *
     */
    private HBox createButtonBox() {
        HBox buttonBox = FXUtils.newHBox();

        Label label = new Label("Trait: ");
        //label.setMaximumSize(new Dimension(1024, 20));
        buttonBox.getChildren().add(label);

//        traitEntry = new JTextField(traitSet.m_sTraitName.get());
//        traitEntry.getDocument().addDocumentListener(new DocumentListener() {
//			
//			@Override
//			public void removeUpdate(DocumentEvent e) {update();}
//			@Override
//			public void insertUpdate(DocumentEvent e) {update();}
//			@Override
//			public void changedUpdate(DocumentEvent e) {update();}
//			void update() {
//				try {
//					traitSet.m_sTraitName.setValue(traitEntry.getText(), traitSet);
//				} catch (Exception e) {
//					// TODO: handle exception
//				}
//			}
//		});
//        traitEntry.setColumns(12);
//        buttonBox.add(traitEntry);
        // buttonBox.add(Box.createHorizontalGlue());

        Button guessButton = new Button("Guess latitude");
        guessButton.setId("Guess latitude");
        guessButton.setOnAction(e->guess(1));
        buttonBox.getChildren().add(guessButton);
        
        Button guessButton2 = new Button("Guess longitude");
        guessButton2.setId("Guess longitude");        
        guessButton2.setOnAction(e->guess(2));
        buttonBox.getChildren().add(guessButton2);


        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e-> {
                try {
                    traitSet.value.setValue("", traitSet);
                } catch (Exception ex) {
                    // TODO: handle exception
                }
                refreshPanel();
        });
        buttonBox.getChildren().add(clearButton);

        m_validateLabel = new SmallLabel("x", "orange");
        m_validateLabel.setVisible(false);
        buttonBox.getChildren().add(m_validateLabel);
        
        return buttonBox;
    } // createButtonBox
    
    
    private void guess(int column) {
        GuessPatternDialog dlg = new GuessPatternDialog(this, m_sPattern);
        //dlg.setName("GuessPatternDialog");
        String sTrait = "";
        switch (dlg.showDialog("Guess traits from taxon names")) {
        case canceled : return;
        case trait: sTrait = dlg.getTrait();
        	break;
        case pattern:
            String sPattern = dlg.getPattern(); 
            try {
                Pattern pattern = Pattern.compile(sPattern);
                for (String sTaxon : sTaxa) {
                    Matcher matcher = pattern.matcher(sTaxon);
                    if (matcher.find()) {
                        String sMatch = matcher.group(1);
                        if (sTrait.length() > 0) {
                            sTrait += ",";
                        }
                        sTrait += sTaxon + "=" + sMatch;
                    }
                    m_sPattern = sPattern;
                }
            } catch (Exception e) {
                return;
            }
            break;
        }
    	String [] strs = sTrait.trim().split(",");
    	for (String str : strs) {
    		String [] strs2 = str.trim().split("=");
    		String taxon = strs2[0].trim();
    		String value = strs2[1].trim();
    		for (int i = 0; i < taxonMapping.size(); i++) {
    			if (taxonMapping.get(i).taxon.equals(taxon)) {
    				if (column == 1) {
    					taxonMapping.get(i).latitude = parseDouble(value);
    				} else {
    					taxonMapping.get(i).longitude = parseDouble(value);
    				}
    				break;
    			}
    		}
    	}
        convertTableDataToTrait();
        if (table != null) {
        	table.refresh();
        }
        validateInput();
        //convertTraitToTableData();
        repaint();
    }
	

    private HBox createButtonBox2() {
        HBox buttonBox = FXUtils.newHBox();

        // buttonBox.add(Box.createHorizontalGlue());

        Button manipulateButton = new Button("Manipulate latitude");
        manipulateButton.setId("Manipulate latitude");
        manipulateButton.setOnAction(e->manipulate(1));
        buttonBox.getChildren().add(manipulateButton);
        
        Button manipulateButton2 = new Button("Manipulate longitude");
        manipulateButton2.setId("Manipulate longitude");        
        manipulateButton2.setOnAction(e->manipulate(2));
        buttonBox.getChildren().add(manipulateButton2);
        return buttonBox;
    } // createButtonBox2
    
    
    // a JavaScript engine
    //static ScriptEngine m_engine = null;
    
//    private void initEngine() {
//		// create a script engine manager
//	    ScriptEngineManager factory = new ScriptEngineManager();
//	    // create a JavaScript engine
//	    m_engine = factory.getEngineByName("nashorn");
//    }

    private void manipulate(int column) {
		String operatee = (column == 1 ? "latitude" : "longitude");
		String formula = (String) Alert.showInputDialog(null, "Give a formula with $x as the " + operatee + 
				" e.g., -$x to make values negative\n" +
				"180+$x to add 180 to " + operatee + "\n" +
				"$x*2+10 to multiply by 2 and add 10\n" +
				"max($x,100) to get the maximum of " + operatee + " and 100", "Manipulate " + operatee, Alert.QUESTION_MESSAGE, "$x");
		if (formula == null || formula.trim().length() == 0) {
			return;
		}
		
		for (int i = 0; i < taxonMapping.size(); i++) {
			String value = (column == 1 ?
					taxonMapping.get(i).latitude.toString():
					taxonMapping.get(i).longitude.toString()
					);
			try {
				value = Double.parseDouble(value) + "";
			} catch (Exception e) {
				value = "0";
			}
			double newValue = value(formula, value);
			if (column == 1) {
					taxonMapping.get(i).latitude = newValue;
			} else {
					taxonMapping.get(i).longitude = newValue;
			}
		}
		convertTableDataToTrait();
		validateInput();
		table.refresh();
	}

	double value(String sFormula, String value) {
//		String sFormula = "with (Math) {" + formula + "}";
		sFormula = sFormula.replaceAll("\\$x", "("+value+")");
		Log.debug.print("parsing " + sFormula);
//		try {
//			if (m_engine == null) {
//				initEngine();
//			}
//			Object o = m_engine.eval(sFormula);
//			String sValue = o.toString();
			double s = ScriptUtil.eval(sFormula);
			Log.debug.println(" = " + s);
			return s;
//		} catch (javax.script.ScriptException es) {
//			return es.getMessage();
//		}
	}

    
	@Override
	public void validateInput() {
		// check all values are specified
		if (taxonMapping == null) {
			return;
		}
        for (int i = 0; i < taxonMapping.size(); i++) {
        	if (Double.isNaN(taxonMapping.get(i).latitude) || Double.isNaN(taxonMapping.get(i).longitude)) {
        		m_validateLabel.setVisible(true);
        		m_validateLabel.setTooltip(new Tooltip("trait for " + taxonMapping.get(i).taxon + " needs to be specified"));
        		// m_validateLabel.repaint();
        		return;
        	}
        }
		m_validateLabel.setVisible(false);
		super.validateInput();
	}
}
