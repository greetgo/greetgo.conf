package kz.greetgo.gbatis.futurecall;

import static kz.greetgo.gbatis.util.SqlUtil.preparePagedSql;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kz.greetgo.gbatis.model.Param;
import kz.greetgo.gbatis.model.Request;
import kz.greetgo.gbatis.model.WithView;
import kz.greetgo.gbatis.util.SqlUtil;
import kz.greetgo.sqlmanager.gen.Conf;
import kz.greetgo.sqlmanager.model.Field;
import kz.greetgo.sqlmanager.model.FieldInfo;
import kz.greetgo.sqlmanager.model.Stru;
import kz.greetgo.sqlmanager.model.Table;

class PreparedSql {
  public String sql;
  public final List<Object> params = new ArrayList<>();
  
  private Conf conf;
  private Stru stru;
  private Request request;
  private Object[] args;
  private Date at;
  private int offset;
  private int pageSize;
  private DbType dbType;
  
  public static PreparedSql prepare(Conf conf, Stru stru, Request request, Object[] args, Date at,
      DbType dbType, int offset, int pageSize) throws Exception {
    
    PreparedSql ret = new PreparedSql();
    
    ret.conf = conf;
    ret.stru = stru;
    ret.request = request;
    ret.args = args;
    ret.at = at;
    ret.dbType = dbType;
    ret.offset = offset;
    ret.pageSize = pageSize;
    
    ret.init();
    
    return ret;
  }
  
  private static class WithSql {
    WithView withView;
    String sql;
  }
  
  private final List<WithSql> withSqls = new ArrayList<>();
  
  private void prepareWithSqls() {
    for (WithView withView : request.withList) {
      withSqls.add(prepareWithSql(withView));
    }
  }
  
  private WithSql prepareWithSql(WithView withView) {
    WithSql ret = new WithSql();
    ret.withView = withView;
    ret.sql = createWithSql(withView);
    return ret;
  }
  
  private String createWithSql(WithView withView) {
    
    String tableName = withView.table.substring(conf.tabPrefix.length());
    Table table = stru.tables.get(tableName);
    if (table == null) throw new IllegalArgumentException("No table " + tableName);
    
    StringBuilder select = new StringBuilder();
    StringBuilder from = new StringBuilder();
    
    select.append("select x.").append(conf.cre);
    for (FieldInfo fieldInfo : table.keyInfo()) {
      select.append(", x.").append(fieldInfo.name);
    }
    from.append(" from " + withView.table + " x");
    
    int index = 1;
    for (String fieldName : withView.fields) {
      int i = index++;
      Field field = getField(table, fieldName);
      from.append(" left join " + fieldView(field) + " x" + i);
      boolean first = true;
      for (FieldInfo fi : table.keyInfo()) {
        if (first) {
          from.append(" on ");
          first = false;
        } else {
          from.append(" and ");
        }
        from.append("x." + fi.name + " = x" + i + "." + fi.name);
      }
      
      select.append(", x" + i + "." + fieldName);
      
    }
    
    select.append(from);
    if (at != null) {
      select.append(" where x." + conf.cre + " <= " + conf.tsTab + "." + conf.ts);
    }
    return select.toString();
  }
  
  private String fieldView(Field field) {
    if (at == null) return conf.vPrefix + field.table.name + '_' + field.name;
    
    {
      StringBuilder sb = new StringBuilder();
      
      sb.append("(select * from ( ");
      
      sb.append("select y.*, row_number() over (partition ");
      
      {
        boolean first = true;
        for (FieldInfo fi : field.table.keyInfo()) {
          if (first) {
            first = false;
          } else {
            sb.append(", ");
          }
          sb.append("y.").append(fi.name);
        }
      }
      
      sb.append(" order by y." + conf.ts + " desc) as rn__ where y." + conf.ts + " <= "
          + conf.tsTab + '.' + conf.ts);
      
      sb.append(" ) yy where yy.rn__ = 1)");
      
      return sb.toString();
    }
  }
  
  private Field getField(Table table, String fieldName) {
    for (Field field : table.fields) {
      if (field.name.equals(fieldName)) return field;
    }
    throw new IllegalArgumentException("No field " + fieldName + " for table " + table.name);
  }
  
  private void init() throws Exception {
    prepareWithSqls();
    
    StringBuilder sb = new StringBuilder();
    
    if (withSqls.size() > 0) {
      appendWithSqls(sb);
      sb.append(' ');
    }
    
    sql = sb + preparePagedSql(dbType, prepareQuestionSql(), offset, pageSize, params);
  }
  
  private void appendWithSqls(StringBuilder sb) {
    sb.append("with ");
    boolean needComma = false;
    if (at != null) {
      sb.append(conf.tsTab + " as (select ? as " + conf.ts
          + (dbType == DbType.Oracle ? " from dual" :"") + ")");
      needComma = true;
      params.add(new java.sql.Timestamp(at.getTime()));
    }
    for (WithSql withSql : withSqls) {
      if (needComma) {
        sb.append(", ");
      } else {
        needComma = true;
      }
      sb.append(withSql.withView.view + " as (" + withSql.sql + ")");
    }
  }
  
  private String prepareQuestionSql() throws Exception {
    StringBuilder sb = new StringBuilder();
    String str = request.sql;
    
    int index = 0;
    while (true) {
      int idx1 = str.indexOf("#{", index);
      if (idx1 < 0) break;
      int idx2 = str.indexOf('}', idx1);
      if (idx2 < 0) throw new IllegalSqlParameterException("Незаконченный параметр в sql: "
          + str.substring(idx1 + 2));
      
      String paramName = str.substring(idx1 + 2, idx2);
      params.add(SqlUtil.forSql(getParamValue(paramName)));
      
      sb.append(str.substring(0, idx1));
      sb.append('?');
      str = str.substring(idx2 + 1);
    }
    
    sb.append(str);
    return sb.toString();
  }
  
  private Object getParamValue(String paramName) throws Exception {
    String[] split = paramName.split("\\.");
    
    Object value = getValueFromArgs(split[0].trim());
    
    for (int i = 1, C = split.length; value != null && i < C; i++) {
      value = getValueFromObject(value, split[i].trim());
    }
    
    return value;
  }
  
  private Object getValueFromObject(Object value, String name) throws Exception {
    String Name = name.substring(0, 1).toUpperCase() + name.substring(1);
    String getterName = "get" + Name;
    String boolGetterName = "is" + Name;
    
    if (value == null) throw new IllegalArgumentException("Cannot get value with name = " + name
        + " from null");
    
    for (Method method : value.getClass().getMethods()) {
      if (method.getParameterTypes().length > 0) continue;
      if (Boolean.TYPE.equals(method.getReturnType())) {
        if (boolGetterName.equals(method.getName())) {
          return method.invoke(value);
        }
      } else {
        if (getterName.equals(method.getName())) {
          return method.invoke(value);
        }
      }
    }
    
    for (java.lang.reflect.Field field : value.getClass().getFields()) {
      if (field.getName().equals(name)) {
        return field.get(value);
      }
    }
    
    throw new IllegalArgumentException("Unknown param name = " + name + " from value = " + value);
  }
  
  private Object getValueFromArgs(String name) throws Exception {
    int index = -1;
    boolean found = false;
    for (Param param : request.paramList) {
      index++;
      if (name.equals(param.name)) {
        found = true;
        break;
      }
    }
    
    if (found) return args[index];
    
    if (args.length == 0) throw new CannotFindParamException("Param name = " + name);
    
    if (args[0] == null) throw new CannotFindParamException("No arguments");
    
    return getValueFromObject(args[0], name);
  }
}
