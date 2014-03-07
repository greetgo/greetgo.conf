package kz.greetgo.gbatis.spring.beans;

import static kz.greetgo.gbatis.util.AbstractWithDbTest.changeDb;

import javax.sql.DataSource;

import kz.greetgo.conf.SysParams;
import kz.greetgo.gbatis.util.AbstractWithDbTest;

import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.mchange.v2.c3p0.ComboPooledDataSource;

@Component
public class DataSourceFab {
  
  @Bean(destroyMethod = "close")
  public DataSource getDataSource() throws Exception {
    if (dataSource == null) createDataSource();
    return dataSource;
  }
  
  private DataSource dataSource;
  
  private void createDataSource() throws Exception {
    ComboPooledDataSource ds = new ComboPooledDataSource();
    ds.setDriverClass("org.postgresql.Driver");
    ds.setJdbcUrl(changeDb(SysParams.pgAdminUrl(), AbstractWithDbTest.userid));
    ds.setUser(AbstractWithDbTest.userid);
    ds.setPassword(AbstractWithDbTest.userid);
    
    ds.setAcquireIncrement(1);
    ds.setMinPoolSize(1);
    ds.setMaxPoolSize(3);
    ds.setMaxIdleTime(120);
    
    this.dataSource = ds;
  }
  
  @Bean
  public JdbcTemplate getJdbcTemplate() throws Exception {
    JdbcTemplate ret = new JdbcTemplate();
    ret.setDataSource(getDataSource());
    return ret;
  }
  
}
