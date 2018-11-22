package mapedit;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import server.game.map.*;
import server.game.units.*;

import javafx.event.*;
import javafx.scene.control.*;
import javafx.scene.canvas.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.stage.FileChooser.*;


public class UserInterface {
  static Logger logger = Logger.getLogger("main");
  
  // functional stuff
  Editor ed;
  Painter painter;
  OperationBox currentColor;
  Operation composed;
  
  // states of operations
  HeightSetter leveler;
  HeightAdjuster digger;
  Copy copier;
  
  // map visualization
  Canvas canvas;
  
  // top bar
  MenuItem resize, fileOpen, fileSave;
  Menu fileMenu;
  MenuBar top;
  
  // pallette
  Button plains, forest, water;
  Button level, dig;
  Button atkWarrior, atkArcher, defWarrior, defArcher, unitRemove;
  Button copy;
  
  // tools
  Button rectangle, oneTimer, normal, paste;
  
  // the container that has it all
  VBox container;
  
  // lock object for synchronization
  Object lock;
  
  // main stage for showing dialogs
  Stage stage;
  
  /** Event handler that updates 'current color'. */
  class ChangeColorOnAction implements EventHandler<ActionEvent> {
    Operation sub;
    ChangeColorOnAction (Operation sub0) {
      sub = sub0;
    }
    @Override
    public void handle (ActionEvent event) {
      currentColor.set(sub);
    }
  }
  /** Event handler for canvas. When mouse is clicked/pressed/dragged/...
   * apply the composed operation at that location. */
  class CanvasHandler implements EventHandler<MouseEvent> {
    @Override
    public void handle (MouseEvent event) {
      synchronized (lock) {
        Position pos = painter.getPosition(event.getX(), event.getY());
        if (composed != null) {
          composed.apply(pos, ed);
        }
      }
    }
  }
  CanvasHandler canvasHandler = new CanvasHandler();
  
  /** Handler for the resize operation. Opens a file dialog awaiting
   * two numbers. */
  class ResizeHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle (ActionEvent event) {
      synchronized (lock) {
        TextInputDialog dialog = new TextInputDialog(String.format("%d %d", ed.numRows(), ed.numCols()));
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
          Scanner sc = new Scanner(result.get());
          try {
            int r = sc.nextInt();
            int c = sc.nextInt();
            int lim = Math.max(r, c);
            canvas.setWidth((double)c / lim * 500.0);
            canvas.setHeight((double)r / lim * 500.0);
            ed.resize(r, c);
          }
          catch (NoSuchElementException exc) {
            logger.info("During resize operation, I expected two numbers, but got something else");
          }
        }
      }
    }
  }
  ResizeHandler resizeHandler = new ResizeHandler();
  
  /** Handler for fileOpen: opens a file dialog and loads the selected
   * file into the editor. */
  class FileOpenHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle (ActionEvent event) {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Open map file...");
      fileChooser.getExtensionFilters().addAll(new ExtensionFilter("All Files", "*.*"));
      File selectedFile = fileChooser.showOpenDialog(stage);
      if (selectedFile == null) {
        return;
      }
      synchronized (lock) {
        try (Scanner sc = new Scanner(selectedFile)) {
          ed.loadFrom(sc);
        }
        catch (FileNotFoundException exc) {
          logger.info("Couldn't load from file to load from, cause file not found");
        }
      }
    }
  };
  FileOpenHandler fileOpenHandler = new FileOpenHandler();
  
  /** Handler for fileSave: opens a file dialog and saves the editor's
   * contents into the selected file. */
  class FileSaveHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle (ActionEvent event) {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Save as...");
      fileChooser.getExtensionFilters().addAll(new ExtensionFilter("All Files", "*.*"));
      File destination = fileChooser.showSaveDialog(stage);
      if (destination == null) {
        return;
      }
      synchronized (lock) {
        try (PrintStream ps = new PrintStream(destination)) {
          ps.print(ed.toString());
        }
        catch (FileNotFoundException exc) {
          logger.info("Couldn't write to file");
        }
      }
    }
  }
  FileSaveHandler fileSaveHandler = new FileSaveHandler();
  
  public UserInterface (Stage stage0) {
    lock = new Object();
    stage = stage0;
    currentColor = new OperationBox();
    
    // terrain modifiers
    plains = new Button("Plains");
    plains.setOnAction(new ChangeColorOnAction(new TerrainPainter(Terrain.Type.PLAINS)));
    forest = new Button("Forest");
    forest.setOnAction(new ChangeColorOnAction(new TerrainPainter(Terrain.Type.FOREST)));
    water = new Button("Water");
    water.setOnAction(new ChangeColorOnAction(new TerrainPainter(Terrain.Type.WATER)));
    
    // height modifiers
    leveler = new HeightSetter(0);
    level = new Button("Level 0");
    level.setOnAction(new ChangeColorOnAction(leveler));
    level.setOnScroll(new EventHandler<ScrollEvent>() {
      @Override
      public void handle (ScrollEvent event) {
        leveler.change(event.getDeltaY() / 200.0);
        level.setText(String.format("Level %d", (int)leveler.target));
      }
    });
    digger = new HeightAdjuster(1);
    dig = new Button("Dig -1");
    dig.setOnAction(new ChangeColorOnAction(digger));
    dig.setOnScroll(new EventHandler<ScrollEvent>() {
      @Override
      public void handle (ScrollEvent event) {
        digger.change(event.getDeltaY() / 200.0);
        dig.setText(String.format("Dig %d", -(int)digger.mod));
      }
    });
    
    // unit modifiers
    atkWarrior = new Button("Atk Warrior");
    atkWarrior.setOnAction(new ChangeColorOnAction(new UnitPutter(1, Unit.Type.WARRIOR)));
    atkArcher = new Button("Atk Archer");
    atkArcher.setOnAction(new ChangeColorOnAction(new UnitPutter(1, Unit.Type.ARCHER)));
    defWarrior = new Button("Def Warrior");
    defWarrior.setOnAction(new ChangeColorOnAction(new UnitPutter(0, Unit.Type.WARRIOR)));
    defArcher = new Button("Def Archer");
    defArcher.setOnAction(new ChangeColorOnAction(new UnitPutter(0, Unit.Type.ARCHER)));
    unitRemove = new Button("Remove unit");
    unitRemove.setOnAction(new ChangeColorOnAction(new UnitRemover()));
    
    // tools: rectangle, oneTimer and normal
    rectangle = new Button("Rectangle fill");
    rectangle.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle (ActionEvent event) {
        synchronized (lock) {
          composed = new Rectangular(currentColor);
        }
      }
    });
    oneTimer = new Button("One-timer");
    oneTimer.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle (ActionEvent event) {
        synchronized (lock) {
          composed = new OneTimer(currentColor);
        }
      }
    });
    normal = new Button("Pencil");
    normal.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle (ActionEvent event) {
        synchronized (lock) {
          composed = currentColor;
        }
      }
    });
    
    // copy and paste tools
    copy = new Button("Copy");
    copy.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle (ActionEvent event) {
        copier.clear();
        currentColor.set(copier);
      }
    });
    paste = new Button("Paste");
    paste.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle (ActionEvent event) {
        synchronized (lock) {
          composed = new Paste(copier);
        }
      }
    });
    
    // the most functional stuff
    canvas = new Canvas(500, 500);
    ed = new Editor(10, 10);
    painter = new Painter(canvas, ed);
    canvas.setOnMousePressed(canvasHandler);
    canvas.setOnMouseDragged(canvasHandler);
    
    // menubar
    resize = new MenuItem("Resize...");
    resize.setOnAction(resizeHandler);
    fileOpen = new MenuItem("Open...");
    fileOpen.setOnAction(fileOpenHandler);
    fileSave = new MenuItem("Save as...");
    fileSave.setOnAction(fileSaveHandler);
    fileMenu = new Menu("File...");
    fileMenu.getItems().addAll(resize, fileOpen, fileSave);
    top = new MenuBar();
    top.getMenus().addAll(fileMenu);
    
    // put it all into the pane
    VBox terrainButts = new VBox(5, plains, forest, water);
    VBox heightButts = new VBox(5, level, dig);
    VBox butts1 = new VBox(20, terrainButts, heightButts);
    VBox unitButts = new VBox(5, atkWarrior, atkArcher, defWarrior, defArcher, unitRemove);
    VBox specialButts = new VBox(/*copy*/);
    VBox butts2 = new VBox(20, unitButts, specialButts);
    VBox toolButts = new VBox(5, rectangle, oneTimer, normal/*, paste*/);
    HBox palette = new HBox(20, butts1, butts2, toolButts);
    HBox bottom = new HBox(20, canvas, palette);
    container = new VBox(top, bottom);
  }
  
  /** Returns the pane that represents the user interface. */
  public Pane getPane () {
    return container;
  }
}
