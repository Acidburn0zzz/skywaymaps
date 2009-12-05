package sky.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.ToggleButton;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.data.SimpleStore;
import com.gwtext.client.data.Store;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.TabPanel;
import com.gwtext.client.widgets.Toolbar;
import com.gwtext.client.widgets.ToolbarButton;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;
import com.gwtext.client.widgets.form.ComboBox;
import com.gwtext.client.widgets.form.FormPanel;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.layout.FitLayout;
import com.gwtext.client.widgets.layout.HorizontalLayout;
import com.gwtext.client.widgets.layout.VerticalLayout;
import com.smartgwt.client.widgets.Slider;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Skywalker implements EntryPoint {

	// ipod - 320W x 480H
	private final int WIDTH = 300;
	private final int HEIGHT = 460;
	private final String FAV_LINK_TEXTSTYLE = "font-family: Verdana;color: blue;font-size: 20px;text-align: center;text-decoration: underline;";
	private final String[][] DIRECTION_DATA = new String[][] {
			new String[] { "[Current Location]" }, new String[] { "Target Plaza" }, new String[] { "Target Store" },
			new String[] { "Target Center" }, new String[] { "Macy's" } };
	private final String[][] PACE_DATA = new String[][] {
			new String[] { "Slow" }, new String[] { "Medium" },
			new String[] { "Fast" } };

	//maps stuff
	private final int CENTER = 0;
	private final int UP = 1;
	private final int LEFT = 2;
	private final int RIGHT = 3;
	private final int DOWN = 4;
	private final int NO_DIRECTION = 0;
	private final int TARGET_MACY = 1;
	private final int TARGET_TARGET = 2;
	private final int TARGET_MACY_30 = 3;
	private boolean zoomedIn = false;
	private boolean showLocation = false;
	private int currentLocation = CENTER;
	private int currentDirection = NO_DIRECTION;

	// various widgets - can be used during updates
	final Panel mapMainPanel = new Panel();
	final Panel mainPannel = new Panel("Skywalker Demo");
	final Panel directionPanel = new Panel("Directions");
	final Panel mapPanel = new Panel("Map");
	final Panel favoritePanel = new Panel("Favorites");
	final ScrollPanel mapScroll = new ScrollPanel();
	final ComboBox toCombo = new ComboBox();
	final ComboBox fromCombo = new ComboBox();
	final Slider zoomBar = new Slider();
	final SpinnerItem spinner = new SpinnerItem();

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		// panel inside the main window
		mainPannel.setBorder(true);

		buildApplication();

		// load default starting point
		mainPannel.add(directionPanel);

		RootPanel.get().add(mainPannel);

		// updates the gui
		mainPannel.setSize(WIDTH, HEIGHT);
		mainPannel.doLayout();
	}

	private void buildApplication() {
		buildMapPanel();
		buildDirectionPanel();
		Panel locationPanel = buildLocationPanel();
		buildFavoritePanel();

		// setup bottom toggles on main window
		Toolbar bottomBar = buildBottomToggleBar(mainPannel, mapPanel,
				locationPanel, favoritePanel);
		mainPannel.setBottomToolbar(bottomBar);
	}

	/**
	 * Builds the main toggle buttons that belong on the bottom of the main
	 * screen. These toggles have listeners that clear the main window and show
	 * the given panels when clicked.
	 * 
	 * @param mainWindowPannel
	 * @param mapPanel
	 * @param directionPanel
	 * @param locationPanel
	 * @param favoritePanel
	 * @param bottomBar
	 */
	private Toolbar buildBottomToggleBar(final Panel mainWindowPannel,
			final Panel mapPanel,
			final Panel locationPanel, final Panel favoritePanel) {

		Toolbar bottomBar = new Toolbar();

		final ToolbarButton directionsToggle = buildToggle(bottomBar,
				"Directions");
		final ToolbarButton locationToggle = buildToggle(bottomBar,
				"Get Location");
		final ToolbarButton favoriteToggle = buildToggle(bottomBar, "Favorites");
		
		//directions is default
		directionsToggle.setPressed(true);

		directionsToggle.addListener(new ButtonListenerAdapter() {
			public void onClick(Button button, EventObject e) {
				// button actions
				if (button.isPressed()) {
					// unpress other buttons
					locationToggle.setPressed(false);
					favoriteToggle.setPressed(false);
					// rebuild and show direction panel
					showPanel(directionPanel);
				} else {
					// hide direction panel
					showPanel(mapPanel);
				}
			}
		});

		locationToggle.addListener(new ButtonListenerAdapter() {
			public void onClick(Button button, EventObject e) {
				// button actions
				if (button.isPressed()) {
					// unpress other buttons
					directionsToggle.setPressed(false);
					favoriteToggle.setPressed(false);
					// show location panel
					showLocation = true;
					updateMap();
					showPanel(locationPanel);
				} else {
					// hide location panel
					showPanel(mapPanel);
				}
			}
		});

		favoriteToggle.addListener(new ButtonListenerAdapter() {
			public void onClick(Button button, EventObject e) {
				// button actions
				if (button.isPressed()) {
					// unpress other buttons
					directionsToggle.setPressed(false);
					locationToggle.setPressed(false);
					// show favorite panel
					showPanel(favoritePanel);
				} else {
					// hide favorite panel
					showPanel(mapPanel);
				}
			}

		});

		return bottomBar;
	}

	private ToolbarButton buildToggle(Toolbar bottomBar, String title) {
		ToolbarButton toggle = new ToolbarButton(title);
		toggle.setEnableToggle(true);
		toggle.setPressed(false);
		toggle.setMinWidth((WIDTH / 3));
		bottomBar.addButton(toggle);
		return toggle;
	}

	private void buildFavoritePanel() {
		TabPanel tabs = new TabPanel();
		tabs.setResizeTabs(false);
		tabs.setBorder(false);

		tabs.add(buildWalkTab());
		tabs.add(buildLocationTab());

		favoritePanel.add(tabs);
		favoritePanel.setBorder(false);
	}

	private Panel buildLocationTab() {
		Panel tab = new Panel("Locations");
		tab.setLayout(new VerticalLayout(15));
		tab.setBorder(false);

		tab.add(buildFavoriteEntry("(1)  Macy's"));
		tab.add(buildFavoriteEntry("(2)  Target Plaza"));
		tab.add(buildFavoriteEntry("(3)  Target Store"));

		return tab;
	}

	private Panel buildWalkTab() {
		Panel tab = new Panel("Walks");
		tab.setLayout(new VerticalLayout(15));
		tab.setBorder(false);

		tab.add(buildFavoriteEntry("(1)  Macy's to Target Plaza"));
		tab.add(buildFavoriteEntry("(2)  Target Plaza to Target Store"));

		return tab;
	}

	private Panel buildFavoriteEntry(final String entryTitle) {
		// an entry in the favorite list has a button with the entry title
		final ToggleButton button = new ToggleButton(entryTitle);
		// delete image is 25 pixels high too!
		button.setPixelSize(225, 25);
		button.addClickHandler(new ClickHandler() {

			public void onClick(ClickEvent event) {
				// TODO Auto-generated method stub
				System.out.println("Toggle clicked: " + entryTitle);
				button.setDown(false);
			}
		});

		// the entry also has an clickable image to delete the entry
		final ToggleButton deleteImage = new ToggleButton(new Image(
				"images/delete.png"));
		deleteImage.setPixelSize(25, 25);
		deleteImage.addClickHandler(new ClickHandler() {

			public void onClick(ClickEvent event) {
				// TODO Auto-generated method stub
				System.out.println("Delete image clicked");
				deleteImage.setDown(false);
			}
		});

		// create the new panel and add componenets
		Panel entry = new Panel();
		entry.setBorder(false);
		entry.setHeader(false);
		entry.setLayout(new HorizontalLayout(10));
		entry.setMargins(15, 6, 0, 0);

		entry.add(button);
		entry.add(deleteImage);

		return entry;
	}

	private Panel buildLocationPanel() {
		return mapMainPanel;
	}

	private void buildDirectionPanel() {
		directionPanel.setLayout(new VerticalLayout(25));
		directionPanel.setMargins(10, 10, 0, 0);
		directionPanel.setBorder(false);

		directionPanel.add(buildSearchBox(fromCombo, "From:"));
		directionPanel.add(buildSearchBox(toCombo, "To:"));
		directionPanel.add(buildPace());
		directionPanel.add(buildRadioOpts());
		directionPanel.add(buildGetMap(mainPannel));
	}

	private Panel buildSearchBox(ComboBox cb, String title) {
		final Store store = new SimpleStore(new String[] { "location" },
				DIRECTION_DATA);
		store.load();

		cb.setMinChars(1);
		// we have a custom label
		cb.setHideLabel(true);
		cb.setStore(store);
		cb.setDisplayField("location");
		cb.setMode(ComboBox.LOCAL);
		cb.setTriggerAction(ComboBox.ALL);
		cb.setEmptyText("Enter location");
		cb.setLoadingText("Searching...");
		cb.setTypeAhead(true);
		cb.setSelectOnFocus(true);
		cb.setForceSelection(true);
		cb.setEditable(true);
		cb.setAllowBlank(false);
		cb.setPageSize(10);
		
		cb.setWidth(230);

		FormPanel form = new FormPanel();
		form.setMargins(0, 0, 25, 0);
		form.setWidth(280);
		form.setBorder(false);
		form.add(cb);

		Panel p = buildRowPanel();
		p.add(form);
		
		Panel p2 = buildRowPanel();
		Label l = new Label(title);
		l.setWidth("40");
		p2.add(l);
		
		Panel wrapper = new Panel();
		wrapper.setLayout(new VerticalLayout(10));
		wrapper.add(p2);
		wrapper.add(p);

		return wrapper;
	}

	private Panel buildPace() {
		final Store store = new SimpleStore(new String[] { "pace" }, PACE_DATA);
		store.load();

		Label pace = new Label("Pace:");
		pace.setPixelSize(40, 25);

		ComboBox cb = new ComboBox();
		cb.setHideLabel(true);
		cb.setStore(store);
		cb.setDisplayField("pace");
		cb.setMode(ComboBox.LOCAL);
		cb.setTriggerAction(ComboBox.ALL);
		cb.setEmptyText("Enter desired pace");
		cb.setLoadingText("Searching...");
		cb.setTypeAhead(true);
		cb.setSelectOnFocus(true);
		cb.setForceSelection(true);
		cb.setEditable(false);
		cb.setAllowBlank(false);
		cb.setWidth(100);
		cb.setPageSize(0);
		cb.setValue("Medium");

		FormPanel form = new FormPanel();
		form.setLabelWidth(40);
		form.setBorder(false);
		form.add(cb);
		form.setWidth(200);
		form.setAutoScroll(false);

		Panel p = buildRowPanel();
		p.add(pace);
		p.add(form);

		return p;
	}

	private Panel buildRowPanel() throws IllegalStateException {
		Panel p = new Panel("", WIDTH-15, 25);
		p.setBorder(false);
		p.setHeader(false);
		p.setLayout(new HorizontalLayout(10));
//		p.setAutoWidth(true); TODO
		return p;
	}

	private Panel buildRadioOpts() {
		spinner.setDefaultValue(0.5);
		spinner.setMin(0);
		spinner.setMax(10);
		spinner.setStep(0.1);
		spinner.setShowTitle(false);
		spinner.setWidth(50);
		
		RadioButton miles = new RadioButton("myRadioButton", "Miles");
		miles.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				System.out.println("miles clicked");
				currentDirection = TARGET_MACY;
				spinner.setDefaultValue(0.5);
				spinner.setMin(0);
				spinner.setMax(10);
				spinner.setStep(0.1);
				spinner.setValue(0.5);
			}
		});
		miles.setChecked(true);
		RadioButton minutes = new RadioButton("myRadioButton", "Minutes");
		minutes.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				System.out.println("minutes clicked");
				currentDirection = TARGET_MACY_30;
				spinner.setDefaultValue(30);
				spinner.setMin(0);
				spinner.setMax(180);
				spinner.setStep(5);
				spinner.setValue(30);
			}
		});

		Panel milesWrapper = new Panel();
		milesWrapper.setLayout(new FitLayout());
		milesWrapper.setPixelSize(75, 25);
		milesWrapper.setBorder(false);
		milesWrapper.add(miles);
		
		Panel minutesWrapper = new Panel();
		minutesWrapper.setLayout(new FitLayout());
		minutesWrapper.setPixelSize(75, 25);
		minutesWrapper.setBorder(false);
		minutesWrapper.add(minutes);
		
		Panel panel = buildRowPanel();
		panel.add(milesWrapper);
		panel.add(minutesWrapper);
		
		DynamicForm df = new DynamicForm();
		df.setFields(spinner);
		panel.add(df);
		
		return panel;
	}

	private Panel buildGetMap(final Panel mainWindowPannel) {
		final ToggleButton save = new ToggleButton("Save");
		save.setPixelSize(50, 20);
		save.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				//TODO to be implemented (if needed)
				System.out.println("Save button clicked");
				save.setDown(false);
			}
		});
		final ToggleButton getMap = new ToggleButton("Get Map");
		getMap.setPixelSize(100, 20);
		getMap.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (currentDirection == NO_DIRECTION) {
					currentDirection = TARGET_MACY;
				}
				updateMap();
				getMap.setDown(false);
			}
		});
		Panel panel = buildRowPanel();
		panel.add(save);
		panel.add(getMap);
		return panel;
	}

	private Panel buildMapPanelOld() {
		Panel topPanel = buildTopPanel();
		Panel mapPanel = buildMap();
		mapMainPanel.add(topPanel);
        mapMainPanel.add(mapPanel);
        return mapMainPanel;
	}
	
	private void updateMap() {
		mapMainPanel.removeAll();
		Panel topPanel = buildTopPanel();
		Panel mapPanel = buildMap();
		topPanel.show();
		mapPanel.show();
		mapMainPanel.add(topPanel);
		mapMainPanel.add(mapPanel);
		showPanel(mapMainPanel);
		System.out.println("Zoom: " + zoomedIn);
		System.out.println("Location: " + currentLocation);
		System.out.println("ShowLocation: " + showLocation);
	}
	
	private Panel buildTopPanel() {
		Panel topPanel = new Panel();
		topPanel.setLayout(new HorizontalLayout(50));

        //create panning images button
		Panel panPanel = new Panel();

        final ToggleButton upImage = new ToggleButton(new Image("images/up.jpg"));
		upImage.setPixelSize(10, 10);
		upImage.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (zoomedIn && !showLocation && currentLocation == CENTER) {
					currentLocation = UP;
					updateMap();
				} else if (zoomedIn && !showLocation && currentLocation == DOWN) {
					currentLocation = CENTER;
					updateMap();
				}
				System.out.println("Up clicked");
				upImage.setDown(false);
			}
		});
		Panel upPanPanel = new Panel();
		upPanPanel.setPaddings(0, 15, 0, 0);
		upPanPanel.add(upImage);

		Panel leftRightPanPanel = new Panel();
		leftRightPanPanel.setLayout(new HorizontalLayout(15));
		final ToggleButton leftImage = new ToggleButton(new Image("images/left.jpg"));
		leftImage.setPixelSize(10, 10);
		leftImage.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (zoomedIn && !showLocation && currentLocation == RIGHT) {
					currentLocation = CENTER;
					updateMap();
				} else if (zoomedIn && !showLocation && currentLocation == CENTER) {
					currentLocation = LEFT;
					updateMap();
				}
				System.out.println("Left clicked");
				leftImage.setDown(false);
			}
		});
		final ToggleButton rightImage = new ToggleButton(new Image("images/right.jpg"));
		rightImage.setPixelSize(10, 10);
		rightImage.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (zoomedIn && !showLocation && currentLocation == LEFT) {
					currentLocation = CENTER;
					updateMap();
				} else if (zoomedIn && !showLocation && currentLocation == CENTER) {
					currentLocation = RIGHT;
					updateMap();
				}
				System.out.println("Right clicked");
				rightImage.setDown(false);
			}
		});
		leftRightPanPanel.add(leftImage);
		leftRightPanPanel.add(rightImage);

		final ToggleButton downImage = new ToggleButton(new Image("images/down.jpg"));
		downImage.setPixelSize(10, 10);
		downImage.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (zoomedIn && !showLocation && currentLocation == UP) {
					currentLocation = CENTER;
					updateMap();
				} else if (zoomedIn && !showLocation && currentLocation == CENTER) {
					currentLocation = DOWN;
					updateMap();
				}
				System.out.println("Down clicked");
				downImage.setDown(false);
			}
		});
		Panel downPanPanel = new Panel();
		downPanPanel.setPaddings(0, 15, 0, 0);
		downPanPanel.add(downImage);

		panPanel.add(upPanPanel);
		panPanel.add(leftRightPanPanel);
		panPanel.add(downPanPanel);
		topPanel.add(panPanel);
				                
        //create zoom images button
		Panel zoomPanel = new Panel();
		if (!zoomedIn) {
			final ToggleButton zoomInImage = new ToggleButton(new Image("images/zoomInButton.jpg"));
			zoomInImage.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					System.out.println("ZoomIn clicked");
					zoomedIn = true;
					zoomInImage.setDown(false);
					updateMap();
				}
			});
			final ToggleButton zoomOutImage = new ToggleButton(new Image("images/zoomOut.jpg"));
			zoomOutImage.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					zoomOutImage.setDown(false);
				}
			});
			zoomPanel.add(zoomInImage);
			zoomPanel.add(zoomOutImage);
		} else {
			final ToggleButton zoomInImage = new ToggleButton(new Image("images/zoomIn.jpg"));
			zoomInImage.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					zoomInImage.setDown(false);
				}
			});
			final ToggleButton zoomOutImage = new ToggleButton(new Image("images/zoomOutButton.jpg"));
			zoomOutImage.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					System.out.println("ZoomOut clicked");
					zoomedIn = false;
					currentLocation = CENTER;
					zoomOutImage.setDown(false);
					updateMap();
				}
			});
			zoomPanel.add(zoomInImage);
			zoomPanel.add(zoomOutImage);
		}
		topPanel.add(zoomPanel);	
		
		//create directions button if needed
		if (currentDirection != NO_DIRECTION) {
			final Panel directionPanel = new Panel();
			final ToggleButton directions = new ToggleButton("Info");
			directions.setPixelSize(50, 20);
			directions.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					System.out.println("Directions clicked");
					//TODO add popup
					// Create the new popup.
        			final PopupPanel popup = new PopupPanel(true);
			        // Position the popup 1/3rd of the way down and across the screen, and
			        // show the popup. Since the position calculation is based on the
			        // offsetWidth and offsetHeight of the popup, you have to use the
			        // setPopupPositionAndShow(callback) method. The alternative would
			        // be to call show(), calculate the left and top positions, and
			        // call setPopupPosition(left, top). This would have the ugly side
			        // effect of the popup jumping from its original position to its
			        // new position.
			        /*popup.setPopupPositionAndShow(new PopupPanel.PositionCallback() {
						public void setPosition(int offsetWidth, int offsetHeight) {
			         		int left = (Window.getClientWidth() - offsetWidth) / 3;
            				int top = (Window.getClientHeight() - offsetHeight) / 3;
			         		popup.setPopupPosition(left, top);
			            }
			        });*/
			        TextField label = new TextField("From: Target\nTo: Macys\nGo Straight\nTurn right at corner\nTurn left");
			        label.setValue("From: Target\nTo: Macys\nGo Straight\nTurn right at corner\nTurn left");
			        popup.add(label);
			        popup.setTitle("Detailed Directions");
			        //topPanel.add(popup);
			        popup.show();
					directions.setDown(false);
				}
			});
			directionPanel.add(directions);
			topPanel.add(directionPanel);
		}
		
		return topPanel;
	}

	//create map	
	private Panel buildMap() {
		Panel mapPanel = new Panel();
		mapPanel.setLayout(new FitLayout());
		Image mapImage = null;
		if (currentDirection == NO_DIRECTION) {
			if (zoomedIn && showLocation) {
				mapImage = new Image("images/TargetPlazaLocation.jpg");
			} else if (zoomedIn && !showLocation) {
				if (currentLocation == CENTER) {
					mapImage = new Image("images/TargetPlaza.jpg");
				} else if (currentLocation == UP) {
					mapImage = new Image("images/TargetPlazaUp.jpg");
				} else if (currentLocation == DOWN) {
					mapImage = new Image("images/TargetPlazaDown.jpg");
				} else if (currentLocation == LEFT) {
					mapImage = new Image("images/TargetPlazaLeft.jpg");
				} else if (currentLocation == RIGHT) {
					mapImage = new Image("images/TargetPlazaRight.jpg");
				}
			} else if (!zoomedIn && showLocation) {
				mapImage = new Image("images/wholeMapLocation.jpg");
			} else if (!zoomedIn && !showLocation) {
				mapImage = new Image("images/wholeMap.jpg");
			}
		} else if (currentDirection == TARGET_MACY) {
			mapImage = new Image("images/TargetPlazaMacysShortest.jpg");
		} else if (currentDirection == TARGET_MACY_30) {
			mapImage = new Image("images/TargetPlazaMacys30Mins.jpg");
		}
		mapPanel.add(mapImage);
		return mapPanel;
	}
	
	private void buildMapPanel(){
		TabPanel tabs = new TabPanel();
		tabs.setResizeTabs(false);
		tabs.setBorder(false);

		tabs.add(buildMapTabScrolling());
		tabs.add(buildDirectionsTab());

		mapPanel.add(tabs);
		mapPanel.setBorder(false);
	}
	
	private Panel buildMapTabScrolling() {
		Panel wrapper = new Panel("View Map");
		wrapper.setLayout(new HorizontalLayout(0));
		wrapper.setBorder(false);
		wrapper.setPixelSize(WIDTH-10, HEIGHT-90);
		
		mapScroll.setAlwaysShowScrollBars(true);
		mapScroll.setPixelSize(270, 360);
		mapScroll.add(new Image("http://images.marketplaceadvisor.channeladvisor.com/hi/59/58567/lotr-leaf-f09.jpg"));
		
		wrapper.add(mapScroll);
		
		return wrapper;
	}
	
	private Panel buildMapTabPanning() {
		Panel wrapper = new Panel("View Map");
		wrapper.setLayout(new VerticalLayout(0));
		wrapper.setBorder(false);
		wrapper.setPixelSize(WIDTH-15, HEIGHT-55);

		Panel topPanel = new Panel();
		topPanel.setLayout(new FitLayout());
		topPanel.setBorder(false);
		topPanel.setPixelSize(WIDTH-15, 25);
		topPanel.add(new Image("images/up.jpg"));
		
		Panel middlePanel = new Panel();
		middlePanel.setLayout(new HorizontalLayout(0));
		middlePanel.setBorder(false);
		middlePanel.setPixelSize(WIDTH-15, HEIGHT-125);
		
		Panel leftPanPanel = new Panel();
		leftPanPanel.setLayout(new FitLayout());
		leftPanPanel.setBorder(false);
		leftPanPanel.setPixelSize(25, HEIGHT-125);
		leftPanPanel.add(new Image("images/left.jpg"));
		
		Panel mapPanel = new Panel();
		mapPanel.setLayout(new FitLayout());
		mapPanel.setBorder(false);
		mapPanel.setPixelSize(WIDTH-65, HEIGHT-125);
		
		Panel rightPanPanel = new Panel();
		rightPanPanel.setLayout(new FitLayout());
		rightPanPanel.setBorder(false);
		rightPanPanel.setPixelSize(25, HEIGHT-125);
		rightPanPanel.add(new Image("images/right.jpg"));
		
		Panel bottomPanel = new Panel();
		bottomPanel.setLayout(new FitLayout());
		bottomPanel.setBorder(false);
		bottomPanel.setPixelSize(WIDTH-15, 25);
		bottomPanel.add(new Image("images/down.jpg"));
		
		middlePanel.add(leftPanPanel);
		middlePanel.add(mapPanel);
		middlePanel.add(rightPanPanel);
		
		wrapper.add(topPanel);
		wrapper.add(middlePanel);
		wrapper.add(bottomPanel);
		
		return wrapper;
	}
	
	private Panel buildDirectionsTab() {
		Label label = new Label("From: Target\nTo: Macys\nGo Straight\nTurn right at corner\nTurn left");
		Panel wrapper = new Panel("View Directions");
		wrapper.add(label);
		return wrapper;
	}

	private Panel buildTestPanel(String testMessage) {
		return buildTestPanel(testMessage, "TestTitle");
	}

	private Panel buildTestPanel(String testMessage, String title) {
		Panel testPanel = new Panel(title);
		testPanel.setLayout(new FitLayout());
		Label test = new Label(testMessage);
		testPanel.add(test);
		return testPanel;
	}

	private void showPanel(Panel panel) {
		mainPannel.removeAll();
		mainPannel.add(panel);
		panel.setVisible(true);
		// updates the gui
		mainPannel.doLayout();
	}

}
