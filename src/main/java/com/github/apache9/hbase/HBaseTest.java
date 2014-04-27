package com.github.apache9.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.regionserver.KeyPrefixRegionSplitPolicy;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * @author zhangduo
 */
public class HBaseTest {

    public static void main(String[] args) throws Exception {
        HBaseTestingUtility util = new HBaseTestingUtility();
        util.startMiniCluster(1);
        HTableDescriptor desc = new HTableDescriptor(
                TableName.valueOf("contacts"));
        desc.addCoprocessor(IndexObserver.class.getName());
        desc.setValue(HTableDescriptor.SPLIT_POLICY,
                KeyPrefixRegionSplitPolicy.class.getName());
        desc.setValue(KeyPrefixRegionSplitPolicy.PREFIX_LENGTH_KEY, "4");
        desc.addFamily(new HColumnDescriptor("def"));
        HTable table = util.createTable(desc, new byte[0][], new Configuration(
                util.getConfiguration()));
        Put put = new Put(Bytes.toBytes("0001_data_0001"));
        put.add(Bytes.toBytes("def"), Bytes.toBytes("name"),
                Bytes.toBytes("zhangduo"));
        put.add(Bytes.toBytes("def"), Bytes.toBytes("phone"),
                Bytes.toBytes("12345678"));
        table.put(put);
        put = new Put(Bytes.toBytes("0001_data_0002"));
        put.add(Bytes.toBytes("def"), Bytes.toBytes("name"),
                Bytes.toBytes("huangdx"));
        put.add(Bytes.toBytes("def"), Bytes.toBytes("phone"),
                Bytes.toBytes("87654321"));
        table.put(put);

        ResultScanner scan = table.getScanner(Bytes.toBytes("def"));
        for (Result result; (result = scan.next()) != null;) {
            String rowKey = Bytes.toString(result.getRow());
            if (rowKey.contains("_data_")) {
                System.out.println("This is a data row for "
                        + rowKey
                        + ", name is "
                        + Bytes.toString(result.getValue(Bytes.toBytes("def"),
                                Bytes.toBytes("name")))
                        + ", phone is "
                        + Bytes.toString(result.getValue(Bytes.toBytes("def"),
                                Bytes.toBytes("phone"))));
            } else {
                System.out.println("This is a index row for "
                        + rowKey
                        + ", primaryKey is "
                        + Bytes.toString(result.getValue(Bytes.toBytes("def"),
                                Bytes.toBytes("pk"))));
            }
        }
        util.shutdownMiniCluster();
    }
}
