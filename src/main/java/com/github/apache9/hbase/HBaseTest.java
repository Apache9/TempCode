package com.github.apache9.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.ipc.CoprocessorRpcChannel;
import org.apache.hadoop.hbase.protobuf.ProtobufUtil;
import org.apache.hadoop.hbase.protobuf.RequestConverter;
import org.apache.hadoop.hbase.protobuf.generated.ClientProtos.GetResponse;
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
        desc.addCoprocessor(IndexEndPoint.class.getName());
        desc.setValue(HTableDescriptor.SPLIT_POLICY,
                KeyPrefixRegionSplitPolicy.class.getName());
        desc.setValue(KeyPrefixRegionSplitPolicy.PREFIX_LENGTH_KEY, "4");
        desc.addFamily(new HColumnDescriptor("def"));
        HTable table = util.createTable(desc, new byte[0][], new Configuration(
                util.getConfiguration()));
        byte[] family = Bytes.toBytes("def");
        byte[] nameQualifier = Bytes.toBytes("name");
        byte[] phoneQualifier = Bytes.toBytes("phone");
        byte[] primaryKeyQualifier = Bytes.toBytes("pk");
        Put put = new Put(Bytes.toBytes("0001_data_0001"));
        put.add(family, nameQualifier, Bytes.toBytes("zhangduo"));
        put.add(family, phoneQualifier, Bytes.toBytes("12345678"));
        table.put(put);
        put = new Put(Bytes.toBytes("0001_data_0002"));
        put.add(family, nameQualifier, Bytes.toBytes("huangdx"));
        put.add(family, phoneQualifier, Bytes.toBytes("87654321"));
        table.put(put);

        ResultScanner scan = table.getScanner(family);
        for (Result result; (result = scan.next()) != null;) {
            String rowKey = Bytes.toString(result.getRow());
            if (rowKey.contains("_data_")) {
                System.out
                        .println("This is a data row for "
                                + rowKey
                                + ", name is "
                                + Bytes.toString(result.getValue(family,
                                        nameQualifier))
                                + ", phone is "
                                + Bytes.toString(result.getValue(family,
                                        phoneQualifier)));
            } else {
                System.out.println("This is a index row for "
                        + rowKey
                        + ", primaryKey is "
                        + Bytes.toString(result.getValue(family,
                                primaryKeyQualifier)));
            }
        }
        scan.close();

        byte[] nameIndexRowKey = Bytes.toBytes("0001_index_name_zhangduo");
        CoprocessorRpcChannel nameGetChannel = table
                .coprocessorService(nameIndexRowKey);

        Get nameGet = new Get(nameIndexRowKey);
        nameGet.addColumn(family, nameQualifier);
        nameGet.addColumn(family, phoneQualifier);
        GetResponse nameGetResponse = HBaseTestProtos.IndexService
                .newBlockingStub(nameGetChannel).get(
                        null,
                        RequestConverter.buildGetRequest(table
                                .getRegionLocation(nameIndexRowKey)
                                .getRegionInfo().getRegionName(), nameGet));
        Result nameGetResult = ProtobufUtil.toResult(nameGetResponse
                .getResult());
        System.out
                .println("Get using name=zhangduo, rowKey is "
                        + Bytes.toString(nameGetResult.getRow())
                        + ", name is "
                        + Bytes.toString(nameGetResult.getValue(family,
                                nameQualifier))
                        + ", phone is "
                        + Bytes.toString(nameGetResult.getValue(family,
                                phoneQualifier)));

        byte[] phoneIndexRowKey = Bytes.toBytes("0001_index_phone_87654321");
        CoprocessorRpcChannel phoneGetChannel = table
                .coprocessorService(phoneIndexRowKey);

        Get phoneGet = new Get(phoneIndexRowKey);
        phoneGet.addColumn(family, nameQualifier);
        phoneGet.addColumn(family, phoneQualifier);
        GetResponse phoneGetResponse = HBaseTestProtos.IndexService
                .newBlockingStub(phoneGetChannel).get(
                        null,
                        RequestConverter.buildGetRequest(table
                                .getRegionLocation(phoneIndexRowKey)
                                .getRegionInfo().getRegionName(), phoneGet));
        Result phoneGetResult = ProtobufUtil.toResult(phoneGetResponse
                .getResult());
        System.out
                .println("Get using phone=87654321, rowKey is "
                        + Bytes.toString(phoneGetResult.getRow())
                        + ", name is "
                        + Bytes.toString(phoneGetResult.getValue(family,
                                nameQualifier))
                        + ", phone is "
                        + Bytes.toString(phoneGetResult.getValue(family,
                                phoneQualifier)));

        table.close();

        util.shutdownMiniCluster();
    }
}
