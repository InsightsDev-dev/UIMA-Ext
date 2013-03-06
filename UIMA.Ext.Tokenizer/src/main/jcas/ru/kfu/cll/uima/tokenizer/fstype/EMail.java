

/* First created by JCasGen Mon Feb 11 15:39:25 MSK 2013 */
package tokenization.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Thu Feb 14 01:21:33 MSK 2013
 * XML source: /home/marsel/workspace/NLP@Cloud/desc/NLP@Cloud_TokenizerDecs.xml
 * @generated */
public class EMail extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(EMail.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated  */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected EMail() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated */
  public EMail(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public EMail(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public EMail(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** <!-- begin-user-doc -->
    * Write your own initialization here
    * <!-- end-user-doc -->
  @generated modifiable */
  private void readObject() {/*default - does nothing empty block */}
     
}

    