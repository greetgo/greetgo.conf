package kz.greepto.gpen.editors.gpen.model

import java.util.List
import java.util.ArrayList
import kz.greepto.gpen.editors.gpen.model.visitor.FigureVisitor

class Combo extends RectFigure {

  public final List<String> lines = new ArrayList;

  public boolean opened
  public boolean autoHeight

  new(String id) {
    super(id)
  }

  new(Combo a) {
    super(a)
    lines += a.lines
    opened = a.opened
    autoHeight = a.autoHeight
  }

  override <T> visit(FigureVisitor<T> v) {
    return v.visitCombo(this);
  }

  override Combo copy() {
    return new Combo(this)
  }
}
