import org.solrmarc.index.SolrIndexer;
import org.marc4j.marc.*;

public class BlahIndexer extends SolrIndexer
{
   //Custom methods for Blacklight@Hull Instance of Solr-marc
   //Simon W Lamb - 13 June 2012
    public BlahIndexer(String propertiesMapFile, String propertyPaths[])
    {
        super(propertiesMapFile, propertyPaths);
    }


  /**
   * Extract the Bibliographic number from the record
   * and remove any occurances of '.' from it, to enable  it 
   * be used as a Solr ID.
   *
   * @param  Record     record
   * @return bibField     Bib number
   */
    public String getBibRecordNo(Record record)
    {
      String bibField = getFirstFieldVal(record, "907a");

      if (bibField != null)
      {
        return bibField.replace(".", "");   
      }
      else
      {
        return bibField;
      }
    }

  /**
   * Return whether the record is unsuppressed or not 
   * if it is suppressed return as null,
   * this method enables us to use the DeleteRecordIfFieldEmpty method
   * in index.properties to delete suppressed fields.` 
   *
   * @param  Record          record
   * @return      
   */
    public String returnSuppressedRecordAsNull(Record record)
    {
      String suppressField = getFirstFieldVal(record, "998f");

      //if the field doesn't exist we will return null..
      if (suppressField == null) {
        return null;
      }
      else {
        if (suppressField.trim().equals("-")) {
          return "unsuppressed";
        }
        else
        {
          return null;
        }
      }
    }
//  other custom indexing functions go here

}
