package kz.greepto.gpen.editors.gpen.prop

import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Collection
import java.util.HashMap
import kz.greepto.gpen.editors.gpen.action.OperGroup
import kz.greepto.gpen.editors.gpen.action.OperModify
import kz.greepto.gpen.util.Handler

import static kz.greepto.gpen.editors.gpen.prop.PropFactory.*

class PropFactory {

  static class AccessorInfo implements PropAccessor, Comparable<AccessorInfo> {
    val String name
    val Object object
    val SceneWorker sceneWorker

    boolean skip = false
    boolean fin = false
    int orderWeightForSet = 1000

    new(String name, Object object, SceneWorker sceneWorker) {
      this.name = name
      this.object = object
      this.sceneWorker = sceneWorker
    }

    var Class<?> type

    override getType() {
      return type
    }

    override compatibleWith(PropAccessor with) {
      if(name != with.name) return false
      return compatibles(type, with.type)
    }

    def static boolean compatibles(Class<?> class1, Class<?> class2) {
      var c1 = class1
      var c2 = class2

      if(c1.primitive) c1 = wrap(c1)
      if(c2.primitive) c2 = wrap(c2)

      return c1 === c2
    }

    def static Class<?> wrap(Class<?> c) {
      if(!c.primitive) throw new RuntimeException('Wrapping can be only for primitive class: ' + c)
      if(c === Byte.TYPE) return Byte
      if(c === Short.TYPE) return Short
      if(c === Integer.TYPE) return Integer
      if(c === Long.TYPE) return Long
      if(c === Float.TYPE) return Float
      if(c === Double.TYPE) return Double
      if(c === Character.TYPE) return Character
      if(c === Boolean.TYPE) return Boolean
      throw new RuntimeException('Unknown primitive class: ' + c)
    }

    override hashCode() {
      val prime = 31;
      var result = 1;
      result = prime * result + name?.hashCode
      result = prime * result + type?.hashCode
      return result;
    }

    override equals(Object obj) {
      if(this === obj) return true
      if(obj === null) return false
      if(!(obj instanceof AccessorInfo)) return false
      var a = obj as AccessorInfo
      if(name != a.name) return false
      if(type != a.type) return false
      return true
    }

    override getName() {
      return name
    }

    ValueGetter getter
    ValueSetter setter

    override getGetter() { getter }

    override getSetter() { setter }

    override toString() {
      var StringBuilder sb = new StringBuilder
      sb.append(name)
      if (setter == null) {
        sb.append(' READONLY')
      }
      if (skip) {
        sb.append(' SKIPPED')
      }
      if (polilines) {
        sb.append(' POLILINES')
      }
      sb.append(' : ' + type.simpleName)
      return sb.toString
    }

    override getValue() {
      return getter.getValue(object)
    }

    boolean polilines = false

    val PropOptions options = new PropOptions() {
      override isPolilines() { polilines }

      override isReadonly() {
        return setter == null
      }

      override orderWeightForSet() {
        return orderWeightForSet
      }

      override operator_add(PropOptions a) { this + a }

      override operator_plus(PropOptions a) { plusOptions(this, a) }
    }

    override getOptions() {
      return options
    }

    override setValue(Object value) {
      var oper = getSettingOper(value)
      if(oper == null) return;
      sceneWorker.applyOper(oper)
    }

    override getSettingOper(Object newValue) {
      if(setter == null) return null
      if(getter == null) throw new NoGetter(name, object.class)
      var curValue = getter.getValue(object)
      if(newValue == curValue) return null
      var ret = new OperModify(setter, newValue, sceneWorker.takeId(object))
      ret.modiPropList = coll.setterList
      return ret
    }

    Collector coll

    override compareTo(AccessorInfo o) {
      return name.compareTo(o.name)
    }

    override addChangeHandler(Handler handler) {
      return sceneWorker.addChangeHandler(handler)
    }

    def void postInit() { if(fin) setter = null }

    override operator_add(PropAccessor a) { this + a }

    override operator_plus(PropAccessor a) { plusPropAccessor(this, a) }

    def PropAccessor plusPropAccessor(PropAccessor x, PropAccessor y) {
      if (!x.compatibleWith(y)) {
        throw new IllegalArgumentException('Plusing of uncompatible AccessorInfo')
      }
      return new PropAccessor() {
        override getType() { x.type }

        override getName() { x.name }

        override getValue() {
          var v = x.value
          if(v == y.value) v else DIFF_VALUES
        }

        override getGetter() { x.getter }

        override getOptions() { x.options + y.options }

        override setValue(Object value) {
          if(DIFF_VALUES === value) return
          var oper = getSettingOper(value)
          if(oper === null) return;
          sceneWorker.applyOper(oper)
        }

        override getSettingOper(Object newValue) {
          var xsetter = x.getSettingOper(newValue)
          var ysetter = y.getSettingOper(newValue)
          if(xsetter === null) return ysetter
          if(ysetter === null) return xsetter
          return new OperGroup(#[xsetter, ysetter], 'Group2')
        }

        override getSetter() { x.setter }

        override addChangeHandler(Handler handler) {
          sceneWorker.addChangeHandler(handler)
        }

        override compatibleWith(PropAccessor with) { x.compatibleWith(with) && y.compatibleWith(with) }

        override operator_add(PropAccessor a) { this + a }

        override operator_plus(PropAccessor a) { plusPropAccessor(this, a) }
      }
    }

  }

  private static class Collector {
    val map = new HashMap<String, AccessorInfo>

    def void put(String name, AccessorInfo info) {
      map.put(name, info)
      __setterList__ = null
    }

    PropList __setterList__ = null

    def AccessorInfo get(String name) {
      return map.get(name)
    }

    def PropList getSetterList() {
      if (__setterList__ === null) {
        __setterList__ = PropList.from(
          map.values.filter[!options.readonly].sortWith [ PropAccessor a, PropAccessor b |
            var sow = a.options.orderWeightForSet - b.options.orderWeightForSet
            if(sow != 0) return sow
            return a.name.compareTo(b.name)
          ].map[it as PropAccessor])
      }
      return __setterList__
    }
  }

  def static PropList parseObject(Object object, SceneWorker sceneWorker) {
    val collector = new Collector

    for (f : object.class.fields) {
      appendField(collector, f, object, sceneWorker)
    }

    for (m : object.class.methods) {
      appendMethod(collector, m, object, sceneWorker)
    }

    return PropList.from(collector.map.values.filter[!skip].sort.map[postInit; it])
  }

  def static <T> PropList parseObjectList(Collection<T> list, SceneWorker sceneWorker) {
    var PropList ret = null

    for (o : list) {
      var u = parseObject(o, sceneWorker)
      if (ret === null) {
        ret = u
      } else {
        ret += u
      }
    }

    return if(ret === null) PropList.empty else ret
  }

  private def static appendMethod(Collector coll, Method m, Object object, SceneWorker sceneWorker) {
    m.accessible = true

    var getter = createMethodValueGetter(m)
    if (getter != null) {

      var info = coll.get(getter.name)
      if (info == null) {
        info = new AccessorInfo(getter.name, object, sceneWorker)
        coll.put(info.name, info)
      }

      info.getter = getter
      info.type = getter.type
      readGetterOptions(info, m)

      return;
    }

    {
      var setter = createMethodValueSetter(m, coll, object.class)
      if(setter == null) return;

      var info = coll.get(setter.name)
      if (info == null) {
        info = new AccessorInfo(setter.name, object, sceneWorker)
        coll.put(info.name, info)
      }
      if(info.skip) return;

      info.setter = setter
      if (info.type == null) {
        info.type = setter.type
      }

      readSetterOptions(info, m)
    }
  }

  private def static appendField(Collector coll, Field f, Object object, SceneWorker sceneWorker) {
    f.accessible = true

    var needRead = true

    {
      var getter = createFieldValueGetter(f)
      if(getter == null) return;

      var info = coll.get(getter.name)
      if (info == null) {
        info = new AccessorInfo(getter.name, object, sceneWorker)
        info.coll = coll
        coll.put(info.name, info)
      }

      info.getter = getter
      info.type = getter.type

      readFieldOptions(info, f)
      needRead = false
    }

    {
      var setter = createFieldValueSetter(f, coll, object.class)
      if(setter == null) return;

      var info = coll.get(setter.name)
      if (info == null) {
        info = new AccessorInfo(setter.name, object, sceneWorker)
        coll.put(info.name, info)
      }

      info.setter = setter
      info.type = setter.type

      if(needRead) readFieldOptions(info, f)
    }
  }

  private def static ValueSetter createMethodValueSetter(Method method, Collector coll, Class<?> klass) {

    if(method.name.length > 3 && "set" != method.name.substring(0, 3)) return null
    if(method.parameterTypes.length != 1) return null

    val propertyName = method.name.substring(3).toFirstLower

    return new ValueSetter() {
      override setValue(Object object, Object value) {
        val info = coll.get(propertyName)
        if(info == null) throw new NoGetter(propertyName, klass)
        val getter = info.getter
        if(getter == null) throw new NoGetter(propertyName, klass)
        var oldValue = getter.getValue(object)
        method.invoke(object, value)
        return oldValue
      }

      override getType() {
        return method.parameterTypes.get(0)
      }

      override getName() {
        return propertyName
      }
    }
  }

  private def static ValueGetter createMethodValueGetter(Method method) {
    if(method.parameterTypes.length > 0) return null
    var String name = null
    if (method.name.length > 3 && "get" == method.name.substring(0, 3)) {
      name = method.name.substring(3).toFirstLower
    }
    if (name == null && method.name.length > 2 && "is" == method.name.substring(0, 2)) {
      if (method.returnType == Boolean || method.returnType == Boolean.TYPE) {
        name = method.name.substring(2).toFirstLower
      }
    }
    if(name == null) return null;
    val propertyName = name
    return new ValueGetter() {
      override getType() {
        return method.returnType
      }

      override getName() {
        return propertyName
      }

      override getValue(Object object) {
        return method.invoke(object)
      }
    }
  }

  private def static ValueGetter createFieldValueGetter(Field field) {
    return new ValueGetter() {
      override getValue(Object object) {
        return field.get(object)
      }

      override getType() {
        return field.type
      }

      override getName() {
        return field.name
      }
    }
  }

  private def static ValueSetter createFieldValueSetter(Field field, Collector coll, Class<?> klass) {
    return new ValueSetter() {
      override getType() {
        return field.type
      }

      override getName() {
        return field.name
      }

      override setValue(Object object, Object value) {
        var info = coll.get(field.name)
        if(info == null) throw new NoGetter(field.name, klass)
        var getter = info.getter
        if(getter == null) throw new NoGetter(field.name, klass)
        var oldValue = getter.getValue(object)
        field.set(object, value)
        return oldValue
      }
    }
  }

  private def static readSetterOptions(AccessorInfo info, Method method) {
    info.skip = method.getAnnotation(Skip) != null

    readPolilines(info, method, false)
    readSetOrderWieght(info, method)
  }

  private def static readFieldOptions(AccessorInfo info, Field field) {
    info.skip = field.getAnnotation(Skip) != null

    info.fin = Modifier.isFinal(field.modifiers)

    readPolilines(info, field, true)
    readSetOrderWieght(info, field)
  }

  private def static readGetterOptions(AccessorInfo info, Method method) {
    info.skip = method.getAnnotation(Skip) != null

    readPolilines(info, method, true)
    readSetOrderWieght(info, method)
  }

  def static readPolilines(AccessorInfo info, AccessibleObject ao, boolean force) {
    if(info.options.polilines && !force) return;
    if (ao.getAnnotation(Polilines) !== null) {
      info.polilines = true
    }
  }

  def static void readSetOrderWieght(AccessorInfo info, AccessibleObject ao) {
    var ann = ao.getAnnotation(SetOrderWeight)
    if(ann === null) return;
    info.orderWeightForSet = ann.value
  }

  private def static PropOptions plusOptions(PropOptions x, PropOptions y) {
    return new PropOptions() {
      override isPolilines() { x.polilines || y.polilines }

      override isReadonly() { x.readonly || y.readonly }

      override operator_add(PropOptions a) { this + a }

      override operator_plus(PropOptions a) { plusOptions(this, a) }

      override orderWeightForSet() { (x.orderWeightForSet + y.orderWeightForSet) / 2 }
    }
  }
}
