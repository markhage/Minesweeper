import java.util.ArrayList;
import java.util.Arrays;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;
import java.util.Random;


// represents a minesweeper game
class MineSweeper extends World {
  ArrayList<Cell> cells;
  int numMines;
  int width;
  int height;
  int screenWidth;
  int screenHeight;
  Random rand;

  // default constructor, the num of mines cannot be more than the total num of cells
  MineSweeper(ArrayList<Cell> cells, int numMines, int width, int height, Random rand) {
    this.cells = cells;
    if (numMines > width * height) {
      this.numMines = width * height;
    } else {
      this.numMines = numMines;
    }
    this.width = width;
    this.height = height;
    this.screenWidth = width * 20;
    this.screenHeight = height * 20;
    this.rand = rand;
  }

  // testing constructor, takes a seeded random object
  MineSweeper(int numMines, int width, int height, Random rand) {
    this.rand = rand;
    if (numMines > width * height) {
      this.numMines = width * height;
    } else {
      this.numMines = numMines;
    }

    this.cells = initMakeCells(numMines, width, height);

    this.width = width;
    this.height = height;
    this.screenWidth = width * 20;
    this.screenHeight = height * 20;

  }

  // player constructor
  MineSweeper(int numMines, int width, int height) {
    this.rand = new Random();
    if (numMines > width * height) {
      this.numMines = width * height;
    } else {
      this.numMines = numMines;
    }

    this.cells = initMakeCells(numMines, width, height);

    this.width = width;
    this.height = height;
    this.screenWidth = width * 20;
    this.screenHeight = height * 20;
  }


  Utils u = new Utils();

  // makes a list of cells, adds the given num of mines randomly
  ArrayList<Cell> initMakeCells(int numMines, int width, int height) {
    int totalNum = width * height;
    int numSafe = totalNum - numMines;
    ArrayList<Cell> cells = new ArrayList<Cell>();
    for (int i = 0; i < numSafe; i++) {
      cells.add(new Safe(new ArrayList<Cell>(), true, false));
    }
    for (int j = 0; j < numMines; j++) {
      int index = this.rand.nextInt(cells.size());
      cells.add(index, new Mine(new ArrayList<Cell>(), true, false));
    }

    u.updateNeighbors(cells, width, height);
    u.updateValues(cells);

    return cells;
  }




  // draws the cells onto the board
  public WorldScene makeScene() {
    WorldScene ws = new WorldScene(this.screenWidth, this.screenHeight);
    u.drawCells(ws, this.cells, this.width, this.height);
    return ws;
  }

  // Effect: when left button is pressed, uncover cells
  // when right button is pressed, flag the cell clicked
  public void onMousePressed(Posn pos, String buttonName) {
    if (buttonName.equals("LeftButton")) {
      u.uncoverCells(this.cells, pos.x, pos.y, this.width);
    } else if (buttonName.equals("RightButton")) {
      u.flagCell(this.cells, pos.x, pos.y, this.width);
    }
  }




  // ends the game when a mine is revealed
  public WorldEnd worldEnds() {
    if (u.isMineRevealed(cells)) {
      return new WorldEnd(true, this.showLose());
    } else if (u.allSafeRevealed(cells)) {
      return new WorldEnd(true, this.showWin());
    } else {
      return new WorldEnd(false, this.makeScene());
    }
  }

  // displays the mine that was clicked, and says "you blew up"
  WorldScene showLose() {
    WorldScene ws = this.makeScene();
    WorldImage image = new OverlayImage(
        new RectangleImage(this.screenWidth, this.screenHeight, OutlineMode.OUTLINE, Color.white),
        new OverlayImage(new TextImage("You blew up", 20, FontStyle.ITALIC, Color.black),
            new RectangleImage(130, 30, OutlineMode.SOLID, Color.white)));
    ws.placeImageXY(image, this.screenWidth / 2, this.screenHeight / 2);
    return ws;
  }

  // tells the player they have won
  WorldScene showWin() {
    WorldScene ws = this.makeScene();
    WorldImage image = new OverlayImage(
        new RectangleImage(this.screenWidth, this.screenHeight, OutlineMode.OUTLINE, Color.white),
        new OverlayImage(new TextImage("You win!!!", 20, FontStyle.ITALIC, Color.black),
            new RectangleImage(100, 30, OutlineMode.SOLID, Color.white)));
    ws.placeImageXY(image, this.screenWidth / 2, this.screenHeight / 2);
    return ws;
  }

}


// represents a type of cell
abstract class Cell {
  ArrayList<Cell> neighbors;
  boolean covered;
  boolean flagged;

  // default constructor, a cell cannot be flagged if it is not covered
  Cell(ArrayList<Cell> neighbors, boolean covered, boolean flagged) {
    this.neighbors = neighbors;
    this.covered = covered;
    if (covered) {
      this.flagged = flagged;
    } else {
      this.flagged = false;
    }
  }

  // is this cell a mine?
  abstract boolean isMine();

  // turns draws this cell
  abstract WorldImage drawCell();

  // Effect: updates this cell's value
  abstract void updateValue();

  // returns a cell's value, mine returns 10
  abstract int getValue();

  // Effect: flags or unflags this cell
  public void flag() {
    if (!this.flagged && this.covered) {
      this.flagged = true;
    } else {
      this.flagged = false;
    }
  }



  //uncover this cell and start flood
  public void uncover() {
    this.uncoverHelp(new ArrayList<Cell>());
  }

  //uncover this cell and start flood
  public void uncoverHelp(ArrayList<Cell> seen) {
    if (this.covered) {
      Utils u = new Utils();
      this.flagged = false;
      this.covered = false;
      if (!this.isMine() && this.getValue() == 0) {
        u.map(this.neighbors, new UpdateNeighbors(seen));
      }
    }
  }
}




// represents a safe cell
class Safe extends Cell {
  int value;

  // default constructor
  Safe(ArrayList<Cell> neighbors, boolean covered, boolean flagged, int value) {
    super(neighbors, covered, flagged);
    this.value = value;
  }

  // testing constructor
  Safe(ArrayList<Cell> neighbors, boolean covered, boolean flagged) {
    super(neighbors, covered, flagged);
    this.value = new Utils().foldl(neighbors, new NumberMines(), 0);
  }

  // this cell is not a mine
  public boolean isMine() {
    return false;
  }

  // effect: changes the value of this cell to the num of mines around it
  public void updateValue() {
    this.value = new Utils().foldl(neighbors, new NumberMines(), 0);
  }

  // draws this safe cell
  public WorldImage drawCell() {
    WorldImage cellBackground = new RectangleImage(18, 18, OutlineMode.SOLID, Color.gray);
    WorldImage cover = new RectangleImage(18, 18, OutlineMode.SOLID, Color.lightGray);
    WorldImage num;
    WorldImage cell;

    if (this.flagged) {
      cell = new OverlayImage(new EquilateralTriangleImage(10, OutlineMode.SOLID, Color.orange),
          cover);
    } else if (this.covered) {
      cell = cover;
    } else if (this.value == 0) {
      cell = cellBackground;
    } else if (this.value == 1) {
      num = new TextImage("1", 12, Color.cyan);
      cell = new OverlayImage(num, cellBackground);
    } else if (this.value == 2) {
      num = new TextImage("2", 12, Color.magenta);
      cell = new OverlayImage(num, cellBackground);
    } else if (this.value == 3) {
      num = new TextImage("3", 12, Color.red);
      cell = new OverlayImage(num, cellBackground);
    } else if (this.value == 4) {
      num = new TextImage("4", 12, Color.yellow);
      cell = new OverlayImage(num, cellBackground);
    } else if (this.value == 5) {
      num = new TextImage("5", 12, Color.green);
      cell = new OverlayImage(num, cellBackground);
    } else if (this.value == 6) {
      num = new TextImage("6", 12, Color.black);
      cell = new OverlayImage(num, cellBackground);
    } else if (this.value == 7) {
      num = new TextImage("7", 12, Color.magenta);
      cell = new OverlayImage(num, cellBackground);
    } else if (this.value == 8) {
      num = new TextImage("8", 12, Color.blue);
      cell = new OverlayImage(num, cellBackground);
    } else {
      num = new TextImage(String.valueOf(this.value), 12, Color.cyan);
      cell = new OverlayImage(num, cellBackground);
    }

    return cell;
  }

  // returns the value of this safe cell
  int getValue() {
    return this.value;
  }
}

// represents a mine cell
class Mine extends Cell {

  // default constructor
  Mine(ArrayList<Cell> neighbors, boolean covered, boolean flagged) {
    super(neighbors, covered, flagged);
  }

  // this is a mine, returns true
  public boolean isMine() {
    return true;
  }

  // draws this mine cell
  public WorldImage drawCell() {
    if (this.flagged) {
      return new OverlayImage(new EquilateralTriangleImage(10, OutlineMode.SOLID, Color.orange),
          new RectangleImage(18, 18, OutlineMode.SOLID, Color.lightGray));
    } else if (this.covered) {
      return new RectangleImage(18, 18, OutlineMode.SOLID, Color.lightGray);
    } else {
      return new OverlayImage(new CircleImage(6, OutlineMode.SOLID, Color.black),
          new RectangleImage(18, 18, OutlineMode.SOLID, Color.red));
    }
  }

  // Effect: does nothing, this does not have a value
  void updateValue() {
    // this is a mine, it has no value
  }

  // this is a mine, returns 10 as a default value to differentiate it from a safe cell
  int getValue() {
    // TODO Auto-generated method stub
    return 10;
  }
}


// represesnts a one arg function X -> Y
interface IFunc<A, R> {
  R apply(A a);
}


// updates a cell to implement flood fill
class UpdateNeighbors implements IFunc<Cell, Cell> {
  ArrayList<Cell> seen;

  UpdateNeighbors(ArrayList<Cell> seen) {
    this.seen = seen;
  }

  // returns an updated.
  // Effect: uncovers the given cell if it is not a mine, recurs if it's value is not 0
  public Cell apply(Cell cell) {
    if (!(cell.isMine()) && cell.getValue() > 0) {
      cell.covered = false;
    }
    if (!(cell.isMine())) {
      seen.add(cell);
      cell.uncoverHelp(seen);
    }
    return cell;
  }
}


// represents a two-arg function
interface IFunc2<A1, A2, R> {
  R apply(A1 arg1, A2 arg2);
}

// determines the number of mines in a list of cell
class NumberMines implements IFunc2<Cell, Integer, Integer> {
  // If given cell is a mine, adds 1 to the given integer
  public Integer apply(Cell arg1, Integer arg2) {
    if (arg1.isMine()) {
      return 1 + arg2;
    } else {
      return arg2;
    }
  }
}


// Utility class
class Utils {
  // applies X, Y -> Y from right to left
  <U, T> U foldl(ArrayList<T> aList, IFunc2<T, U, U> func, U base) {
    for (T t : aList) {
      base = func.apply(t, base);
    }
    return base;
  }


  // iterates over a list and applies a function
  <U, T> ArrayList<U> map(ArrayList<T> aList, IFunc<T, U> func) {
    ArrayList<U> result = new ArrayList<U>();
    for (T t : aList) {
      result.add(func.apply(t));
    }
    return result;
  }


  // Effect: uncovers the clicked cell and does flood fill
  void uncoverCells(ArrayList<Cell> cells, int x, int y, int width) {
    int column = x / 20;
    int row = y / 20;

    int cellidx = (row * width) + column;
    cells.get(cellidx).uncover();
  }


  //Effect: adds a flag to a cell
  void flagCell(ArrayList<Cell> cells, int x, int y, int width) {
    int column = x / 20;
    int row = y / 20;

    int cellidx = (row * width) + column;
    cells.get(cellidx).flag();
  }


  // Effect: updates the values of every cell in the given arraylist<Cell>
  void updateValues(ArrayList<Cell> aList) {
    for (Cell t : aList) {
      t.updateValue();
    }
  }


  // Effect : adds the cells to the world scene
  void drawCells(WorldScene ws, ArrayList<Cell> cells, int width, int height) {
    for (int i = 1; i <= height; i++) {
      int y = (i - 1) * width;
      for (int j = 1; j <= width; j++) {
        Cell currentCell = cells.get(y + j - 1);
        ws.placeImageXY(currentCell.drawCell(), ((j - 1) * 20) + 11, ((i - 1) * 20) + 11);
      }
    }
  }


  // returns true if any of the mines are uncovered
  boolean isMineRevealed(ArrayList<Cell> cells) {
    for (Cell c : cells) {
      if (c.isMine() && !c.covered) {
        return true;
      }
    }
    return false;
  }

  // returns true  if all of the safe cells are revealed
  boolean allSafeRevealed(ArrayList<Cell> cells) {
    for (Cell c : cells) {
      if (c.covered && !c.isMine()) {
        return false;
      }
    }
    return true;
  }

  // Effect : changes each cell's neighbors to be the cells around it
  void updateNeighbors(ArrayList<Cell> cells, int width, int height) {
    for (int i = 1; i <= height; i++) {
      int y = (i - 1) * width;
      for (int j = 1; j <= width; j++) {
        ArrayList<Cell> newNeighbors = new ArrayList<Cell>();
        int currentIndex = (y + j - 1);
        Cell currentCell = cells.get(currentIndex);

        int topLeft = currentIndex - width - 1;
        int top = currentIndex - width;
        int topRight = currentIndex - width + 1;
        int left = currentIndex - 1;
        int right = currentIndex + 1;
        int bottomLeft = currentIndex + width - 1;
        int bottom = currentIndex + width;
        int bottomRight = currentIndex + width + 1;

        if (j > 1) {
          Cell leftCell = cells.get(left);
          newNeighbors.add(leftCell);
        }

        if (j < width) {
          Cell rightCell = cells.get(right);
          newNeighbors.add(rightCell);
        }

        if (i < height) {
          Cell bottomCell = cells.get(bottom);
          newNeighbors.add(bottomCell);
        }

        if (i > 1) {
          Cell topCell = cells.get(top);
          newNeighbors.add(topCell);
        }

        if (j > 1 && i > 1) {
          Cell topLeftCell = cells.get(topLeft);
          newNeighbors.add(topLeftCell);
        }

        if (j < width && i > 1) {
          Cell topRightCell = cells.get(topRight);
          newNeighbors.add(topRightCell);
        }

        if (j > 1 && i < height) {
          Cell bottomLeftCell = cells.get(bottomLeft);
          newNeighbors.add(bottomLeftCell);
        }

        if (j < width && i < height) {
          Cell bottomRightCell = cells.get(bottomRight);
          newNeighbors.add(bottomRightCell);
        }

        currentCell.neighbors = newNeighbors;
      }
    }
  }
}


class ExamplesMineSweeper {
  Utils u = new Utils();

  ArrayList<Cell> mt;
  ArrayList<Cell> hasOne;

  Cell mine;
  Cell safe;
  Cell safe2;
  Cell safe3;
  Cell mine2;
  Cell mine3;

  ArrayList<Cell> hasTwo;

  MineSweeper game1;


  void initExamples() {
    mt = new ArrayList<Cell>();
    hasOne = new ArrayList<Cell>(Arrays.asList(new Mine(mt, true, false)));

    mine = new Mine(mt, false, false);
    safe = new Safe(mt, false, false);
    safe2 = new Safe(mt, false, false);
    safe3 = new Safe(mt, false, false);
    mine2 = new Mine(mt, false, false);
    mine3 = new Mine(mt, false, false);

    hasTwo = new ArrayList<Cell>(Arrays.asList(safe2, mine2, safe3, mine3));

    game1 = new MineSweeper(5, 5, 5, new Random(1));
  }

  void testGameConstructor(Tester t) {
    initExamples();
    Cell testconstructorsafe = new Safe(mt, false, true);
    Cell testconstructormine = new Mine(mt, false, true);

    t.checkExpect(u.foldl(game1.cells, new NumberMines(), 0), 5);
    t.checkExpect(game1.screenHeight, 100);
    t.checkExpect(game1.screenWidth, 100);
    t.checkExpect(testconstructorsafe.flagged, false);
    t.checkExpect(testconstructormine.flagged, false);
  }

  void testIsMine(Tester t) {
    initExamples();
    t.checkExpect(mine.isMine(), true);
    t.checkExpect(safe.isMine(), false);
  }


  void testDrawCell(Tester t) {
    Cell flaggedcell = new Safe(mt, true, true, 0);
    Cell zero = new Safe(mt, false, false, 0);
    Cell one = new Safe(mt, false, false, 1);
    Cell two = new Safe(mt, false, false, 2);
    Cell three = new Safe(mt, false, false, 3);
    Cell four = new Safe(mt, false, false, 4);
    Cell five = new Safe(mt, false, false, 5);
    Cell six = new Safe(mt, false, false, 6);
    Cell seven = new Safe(mt, false, false, 7);
    Cell eight = new Safe(mt, false, false, 8);
    Cell covered = new Safe(mt, true, false, 3);
    Cell drawmine = new Mine(mt, false, false);
    Cell drawflaggedmine = new Mine(mt, true, true);
    Cell drawcoveredmine = new Mine(mt, true, false);

    t.checkExpect(flaggedcell.drawCell(),
        new OverlayImage(new EquilateralTriangleImage(10, OutlineMode.SOLID, Color.orange),
            new RectangleImage(18, 18, OutlineMode.SOLID, Color.lightGray)));
    t.checkExpect(zero.drawCell(), new RectangleImage(18, 18, OutlineMode.SOLID, Color.gray));
    t.checkExpect(one.drawCell(), new OverlayImage(new TextImage("1", 12, Color.cyan),
        new RectangleImage(18, 18, OutlineMode.SOLID, Color.gray)));
    t.checkExpect(two.drawCell(), new OverlayImage(new TextImage("2", 12, Color.magenta),
        new RectangleImage(18, 18, OutlineMode.SOLID, Color.gray)));
    t.checkExpect(three.drawCell(), new OverlayImage(new TextImage("3", 12, Color.red),
        new RectangleImage(18, 18, OutlineMode.SOLID, Color.gray)));
    t.checkExpect(four.drawCell(), new OverlayImage(new TextImage("4", 12, Color.yellow),
        new RectangleImage(18, 18, OutlineMode.SOLID, Color.gray)));
    t.checkExpect(five.drawCell(), new OverlayImage(new TextImage("5", 12, Color.green),
        new RectangleImage(18, 18, OutlineMode.SOLID, Color.gray)));
    t.checkExpect(six.drawCell(), new OverlayImage(new TextImage("6", 12, Color.black),
        new RectangleImage(18, 18, OutlineMode.SOLID, Color.gray)));
    t.checkExpect(seven.drawCell(), new OverlayImage(new TextImage("7", 12, Color.magenta),
        new RectangleImage(18, 18, OutlineMode.SOLID, Color.gray)));
    t.checkExpect(eight.drawCell(), new OverlayImage(new TextImage("8", 12, Color.blue),
        new RectangleImage(18, 18, OutlineMode.SOLID, Color.gray)));
    t.checkExpect(covered.drawCell(),
        new RectangleImage(18, 18, OutlineMode.SOLID, Color.lightGray));
    t.checkExpect(drawmine.drawCell(),
        new OverlayImage(new CircleImage(6, OutlineMode.SOLID, Color.black),
            new RectangleImage(18, 18, OutlineMode.SOLID, Color.red)));
    t.checkExpect(drawflaggedmine.drawCell(),
        new OverlayImage(new EquilateralTriangleImage(10, OutlineMode.SOLID, Color.orange),
            new RectangleImage(18, 18, OutlineMode.SOLID, Color.lightGray)));
    t.checkExpect(drawcoveredmine.drawCell(),
        new RectangleImage(18, 18, OutlineMode.SOLID, Color.lightGray));


  }


  void testNumberMines(Tester t) {
    initExamples();
    t.checkExpect(u.foldl(hasTwo, new NumberMines(), 0), 2);
    t.checkExpect(u.foldl(mt, new NumberMines(), 0), 0);
  }

  void testUpdateNeighbors(Tester t) {
    initExamples();
    t.checkExpect(safe2.neighbors, new ArrayList<Cell>());
    t.checkExpect(mt, new ArrayList<Cell>());
    u.updateNeighbors(hasTwo, 2, 2);
    u.updateNeighbors(mt, 0, 0);
    t.checkExpect(safe2.neighbors, new ArrayList<Cell>(Arrays.asList(mine2, safe3, mine3)));
    t.checkExpect(mt, new ArrayList<Cell>());
  }

  void testGetValue(Tester t) {
    initExamples();

    t.checkExpect(game1.cells.get(0).getValue(), 1);
    t.checkExpect(game1.cells.get(6).getValue(), 10);
    t.checkExpect(game1.cells.get(24).getValue(), 0);
  }

  void testIsMineRevealed(Tester t) {
    initExamples();
    t.checkExpect(u.isMineRevealed(hasTwo), true);
    mine2.covered = true;
    mine3.covered = true;
    t.checkExpect(u.isMineRevealed(hasTwo), false);
    t.checkExpect(u.isMineRevealed(mt), false);

  }

  void testAllSafeRevealed(Tester t) {
    initExamples();
    t.checkExpect(u.allSafeRevealed(hasTwo), true);
    safe3.covered = true;
    t.checkExpect(u.allSafeRevealed(hasTwo), false);
    t.checkExpect(u.allSafeRevealed(mt), true);
  }

  void testMouseHandler(Tester t) {
    initExamples();
    t.checkExpect(game1.cells.get(0).flagged, false);
    t.checkExpect(game1.cells.get(0).covered, true);
    t.checkExpect(game1.cells.get(1).covered, true);
    t.checkExpect(game1.cells.get(6).covered, true);
    t.checkExpect(game1.cells.get(7).covered, true);
    t.checkExpect(game1.cells.get(24).covered, true);
    t.checkExpect(game1.cells.get(23).covered, true);
    t.checkExpect(game1.cells.get(22).covered, true);
    t.checkExpect(game1.cells.get(21).covered, true);

    game1.onMousePressed(new Posn(5, 5), "RightButton");
    t.checkExpect(game1.cells.get(0).flagged, true);
    game1.onMousePressed(new Posn(5, 5), "RightButton");
    t.checkExpect(game1.cells.get(0).flagged, false);
    game1.onMousePressed(new Posn(5, 5), "RightButton");
    game1.onMousePressed(new Posn(5, 5), "LeftButton");
    t.checkExpect(game1.cells.get(0).flagged, false);
    t.checkExpect(game1.cells.get(0).covered, false);
    // testing flood fill
    t.checkExpect(game1.cells.get(1).covered, true);
    t.checkExpect(game1.cells.get(6).covered, true);
    t.checkExpect(game1.cells.get(7).covered, true);

    //testing flood fill
    game1.onMousePressed(new Posn(95, 95), "LeftButton");
    t.checkExpect(game1.cells.get(24).covered, false);
    t.checkExpect(game1.cells.get(23).covered, false);
    t.checkExpect(game1.cells.get(22).covered, false);
    t.checkExpect(game1.cells.get(21).covered, true);
  }


}

class RunMineSweeper {
  void testMinesweeper(Tester t) {
    MineSweeper ms = new MineSweeper(30, 15, 15, new Random(1));
    ms.bigBang(ms.screenWidth, ms.screenHeight, 1);
  }
}

