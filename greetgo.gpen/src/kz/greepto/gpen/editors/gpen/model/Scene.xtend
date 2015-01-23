package kz.greepto.gpen.editors.gpen.model

import java.util.ArrayList
import java.util.List
import kz.greepto.gpen.editors.gpen.model.visitor.FigureVisitor

class Scene extends Figure {

  public List<IdFigure> list = new ArrayList;

  override <T> visit(FigureVisitor<T> v) {
    return v.visitScene(this);
  }

  new() {
  }

  new(Scene a) {
    list += a.list.map[copy as IdFigure]
  }

  override Scene copy() {
    return new Scene(this)
  }

  def IdFigure findById(String id) {
    for (IdFigure x : list) {
      if(id == x.id) return x
    }

    return null
  }

  def IdFigure findByIdOrDie(String id) {
    var ret = findById(id)
    if (ret === null) throw new RuntimeException('No IdFigure with id = ' + id)
    return ret
  }
}
