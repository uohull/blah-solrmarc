import org.solrmarc.index.SolrIndexer;
import org.solrmarc.tools.Utils;
import org.marc4j.marc.*;
import java.util.HashSet.*;
import java.util.*;

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
   * and remove any occurances of '.' from it, to enable it
   * be used as a Solr ID.
   *
   * Additionally if the ID is 9 characters (typically the bib number with control char at the end)
   * chop the last digit/char off to make it 8 chars (this will match the Millennium ID) eg:
   *   - 907a might store 'b10082281' but to remain compatable with Milleniums usage this will be chopped
   *      to b1008228
   *
   * @param  Record     record
   * @return bibField     Bib number
   */
    public String getBibRecordNo(Record record)
    {
      String bibField = getFirstFieldVal(record, "907a");

      if (bibField != null)
      {
        String bibFieldId = bibField.replace(".", "");        

        // Code to chop last char if the id is larger than 8 chars... 
        if (bibFieldId.length() > 8) {
          return bibFieldId.substring(0, bibFieldId.length() - 1);
        }
        else {
          return bibFieldId;
        }   
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

  /**
   * Returns a list of  URL and URL texts for the given Marc record
   * Method uses the 'u' subfield for the URL, and 'z' subfield for the link description
   * Link text defaults to 'Access this resource online' when z is null.
   * Each member of set is in the following form "URL|Link text"
   * Future note: Could return as JSON for clearer def of data. 
   * Cast list is stored in 511a Indicator field One = 1   
   *
   * @param  Record          record
   * @param  String            fieldNumbers
   * @return  Set                 resultSet      
   */
  public Set getFulltextUrls(Record record, String fieldNumbers) {

    Set resultSet = new LinkedHashSet();
    String[] fieldArray;

    fieldArray = getFieldArrayFromString(fieldNumbers);

    List fields = record.getVariableFields(fieldArray);
    
    for (Object field : fields)  {
      if (field instanceof DataField) {

        DataField dField = (DataField)field;

        String url;
        String urlText;

        if (dField.getSubfield('u') != null) {
           url = dField.getSubfield('u').getData();

          if (dField.getSubfield('z') != null) 
          {
             urlText = dField.getSubfield('z').getData();
          }
          else 
          {
             urlText = "Access this resource online";
          }
          resultSet.add(url + "|" + urlText);
        }
      }
    }

    return resultSet;
  }


  /**
   * Returns a Cast list if it exists for a record 
   * Cast list is stored in 511a Indicator field One = 1   
   *
   * @param  LinkedHashSet          resultSet
   * @return      
   */
  public Set getRecordCastList(Record record)
  {
    Set resultSet = new LinkedHashSet();
    List fields = record.getVariableFields("511");

    for (Object field : fields)
    {
      if (field instanceof DataField)
      {
        DataField dField = (DataField)field;
        
        if (dField.getIndicator1() == '1') 
        {
          if (dField.getSubfield('a') != null) 
          {
            resultSet.add(dField.getSubfield('a').getData());
          }          
        }  
      } 
    }
    return resultSet;
  }

 /**
   * Returns a Performers list if it exists for a record 
   * Cast list is stored in 511a Indicator field One != 1   
   *
   * @param  LinkedHashSet          resultSet
   * @return      
   */
  public Set getRecordPerformerList(Record record)
  {
    Set resultSet = new LinkedHashSet();
    List fields = record.getVariableFields("511");

    for (Object field : fields)
    {
      if (field instanceof DataField)
      {
        DataField dField = (DataField)field;
        
        if (dField.getIndicator1() != '1')
        {
          if (dField.getSubfield('a') != null) 
          {
            resultSet.add(dField.getSubfield('a').getData());
          }          
        }  
      } 
    }
    return resultSet;
  }

  /**
   * Enables you to getFields by the first and second indicators, fieldNo and subFieldString (use null for no subfield )
   * fieldNumbers can multiple fields by sperating the fields using the ":" char.  
   *
   * @param  LinkedHashSet          resultSet
   * @return      
   */
   public Set getFieldsByIndicators(Record record, String fieldNumbers, String subFieldString, String firstIndicator, String secondIndicator)
   {
    Set resultSet = new LinkedHashSet();  
    String[] fieldArray;

    fieldArray = getFieldArrayFromString(fieldNumbers);

    //Loop around all the fieldNo
    for (String fieldNo : fieldArray)
    { 
      List fields = record.getVariableFields(fieldNo);

      //Loop around all the fields within the fieldNo
      for (Object field : fields)
      {
        if (field instanceof DataField)
        {
          DataField dField = (DataField) field;
          
          //Compare the dataField with the indicators specified in the method params
          if (dField.getIndicator1() == firstIndicator.charAt(0) && dField.getIndicator2() == secondIndicator.charAt(0))
          {
            //If null has been entered as a subField we get all the subField entries
            if (subFieldString.equals("null")) {
              List subFieldList = dField.getSubfields(); 
    
              for (Object subField : subFieldList)
              {
                if (subField instanceof Subfield)
                {
                  Subfield sField = (Subfield) subField;
                  resultSet.add(sField.getData());
                }
              }
            }
            //Else we just get the subField entry specified 
            else 
            {
              char subFieldChar = subFieldString.charAt(0);
              if (dField.getSubfield(subFieldChar) != null) 
              {
                resultSet.add(dField.getSubfield(subFieldChar).getData());
              }     
            }
          }  
        } 
      }
    }
    return resultSet;
  }

  /**
  * Return the Publication date in 260c or 264c as a string
  * This is an adapted version of getDate, with the fallback of 
  * checking for 264c for RDA records
  * @param record - the marc record object
  * @return 260c, "cleaned" per org.solrmarc.tools.Utils.cleanDate()
  */
  public String getPubDate(Record record)
  {
    String date = getFieldVals(record, "260c", ", ");

    if (date == null || date.length() == 0) {
      date = getFieldVals(record, "264c", ", ");
      if (date == null || date.length() == 0) 
        return (null);
    }  
    return Utils.cleanDate(date);
  }

  //If the fieldNo variable contains more than one fieldNo (split by the ":" char) then add them fieldArray
  //Otherwise just add the single field to the same array
  private String[] getFieldArrayFromString(String fieldNumbers) {
    String[] fieldArray;

    if (fieldNumbers.contains(":")) {
      fieldArray = fieldNumbers.split("\\:");
    }
    else {
      fieldArray = new String[] { fieldNumbers };
    }
    return fieldArray;
  }

}
