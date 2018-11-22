package mapedit;

import java.util.*;
import server.game.Constants;
import server.game.map.*;
import server.game.units.*;

import javafx.scene.canvas.*;
import javafx.scene.paint.*;


/** His responsibility is to draw the map on the screen and do so
 * whenever the underlying terrain changes. */
public class Painter {
  protected Editor ed;
  protected Canvas canvas;
  
  /** Creates a painter that paints to <canvas0> and is associated
   * with the editor <ed>. Whenever that editor makes a change, it
   * tells the canvas to redraw a part of the screen. */
  public Painter (Canvas canvas0, Editor ed0) {
    canvas = canvas0;
    ed = ed0;
    ed.setPainter(this);
  }
  
  double cell_h () {
    return canvas.getHeight() / ed.numRows();
  }
  double cell_w () {
    return canvas.getWidth() / ed.numCols();
  }
  
  /** Returns the cell position associated with the provided pixel. */
  Position getPosition (double x, double y) {
    return new Position((int)(y / cell_h()), (int)(x / cell_w()));
  }
  
  /** Redraws the cell at position <pos>. */
  public void redraw (Position pos) {
    double x = cell_w() * pos.c;
    double y = cell_h() * pos.r;
    GraphicsContext gc = canvas.getGraphicsContext2D();
    
    // paint terrain
    Paint paint;
    Terrain.Type t = ed.terrainAt(pos);
    if (t == Terrain.Type.PLAINS) paint = Paint.valueOf("rgb(0, 255, 0)");
    else if (t == Terrain.Type.FOREST) paint = Paint.valueOf("rgb(0, 128, 0)");
    else if (t == Terrain.Type.WATER) paint = Paint.valueOf("rgb(0, 255, 255)");
    else paint = Paint.valueOf("rgb(0, 0, 0)");
    gc.setFill(paint);
    gc.fillRect(x + 1, y + 1, cell_w() - 2, cell_h() - 2);
    
    // shadow to show height
    int h = ed.heightAt(pos);
    double old = gc.getGlobalAlpha();
    double alpha = (double)(Constants.maxHeight - h) / Constants.maxHeight / 2.0;
    gc.setGlobalAlpha(alpha);
    gc.setFill(Color.BLACK);
    gc.fillRect(x + 1, y + 1, cell_w() - 2, cell_h() - 2);
    gc.setGlobalAlpha(old);
    
    // border
    gc.setStroke(Color.BLACK);
    gc.setLineWidth(1.0);
    gc.strokeRect(x, y, x + cell_w(), y + cell_h());
    
    // paint unit
    InitialUnit unit = ed.unitAt(pos);
    if (unit != null) {
      paint = (unit.owner == Constants.defender ? Color.BLUE : Color.RED);
      gc.setFill(paint);
      if (unit.type == Unit.Type.WARRIOR) {
        double[] xs = {x + cell_w()/2, x + cell_w(), x + cell_w()/2, x};
        double[] ys = {y, y + cell_h()/2, y + cell_h(), y + cell_h()/2};
        gc.fillPolygon(xs, ys, 4);
      }
      if (unit.type == Unit.Type.ARCHER) {
        gc.fillOval(x, y, cell_w(), cell_h());
      }
    }
  }
  public void redraw (int i, int j) {
    redraw(new Position(i, j));
  }
  
  /** Redraws the entire map. */
  public void redrawAll () {
    for (int i = 0; i < ed.numRows(); i++) {
      for (int j = 0; j < ed.numCols(); j++) {
        redraw(i, j);
      }
    }
  }
}
