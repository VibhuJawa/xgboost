/*
 Copyright (c) 2014 by Contributors

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package ml.dmlc.xgboost4j.scala.spark.rapids

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.types._
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.catalyst.util.DateTimeUtils

private[xgboost4j] class RowConverter(schema: StructType) {
  private val converters = schema.fields.map {
    f => RowConverter.getConverterForType(f.dataType)
  }

  final def toExternalRow(row: InternalRow): Row = {
      if (row == null) {
        null
      } else {
        val ar = new Array[Any](row.numFields)
        var idx = 0
        while (idx < row.numFields) {
          ar(idx) = converters(idx).convert(row, idx)
          idx += 1
        }
        new GenericRowWithSchema(ar, schema)
      }
  }
}

private[xgboost4j] object RowConverter {
  private abstract class TypeConverter {
    final def convert(row: InternalRow, column: Int): Any = {
      if (row.isNullAt(column)) null else convertImpl(row, column)
    }

    protected def convertImpl(row: InternalRow, column: Int): Any
  }

  private def getConverterForType(dataType: DataType): TypeConverter = {
    dataType match {
      case BooleanType => BooleanConverter
      case ByteType => ByteConverter
      case ShortType => ShortConverter
      case IntegerType => IntConverter
      case FloatType => FloatConverter
      case LongType => LongConverter
      case DoubleType => DoubleConverter
      case DateType => DateConverter
      case TimestampType => TimestampConverter
      case unknown => throw new UnsupportedOperationException(
        s"Type $unknown not supported")
    }
  }

  private object BooleanConverter extends TypeConverter {
    override def convertImpl(row: InternalRow, column: Int): Any =
      row.getBoolean(column)
  }

  private object ByteConverter extends TypeConverter {
    override def convertImpl(row: InternalRow, column: Int): Any =
      row.getByte(column)
  }

  private object ShortConverter extends TypeConverter {
    override def convertImpl(row: InternalRow, column: Int): Any =
      row.getShort(column)
  }

  private object IntConverter extends TypeConverter {
    override def convertImpl(row: InternalRow, column: Int): Any =
      row.getInt(column)
  }

  private object FloatConverter extends TypeConverter {
    override def convertImpl(row: InternalRow, column: Int): Any =
      row.getFloat(column)
  }

  private object LongConverter extends TypeConverter {
    override def convertImpl(row: InternalRow, column: Int): Any =
      row.getLong(column)
  }

  private object DoubleConverter extends TypeConverter {
    override def convertImpl(row: InternalRow, column: Int): Any =
      row.getDouble(column)
  }

  private object DateConverter extends TypeConverter {
    override def convertImpl(row: InternalRow, column: Int): Any =
      DateTimeUtils.toJavaDate(row.getInt(column))
  }

  private object TimestampConverter extends TypeConverter {
    override def convertImpl(row: InternalRow, column: Int): Any =
      DateTimeUtils.toJavaTimestamp(row.getLong(column))
  }
}