import java.util.ArrayList;

public class TxHandler 
{
	
	private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) 
    {
    	this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) 
    {    	
    	if (tx == null) 
    		return false;
    	
    	double totalInValue  = 0;
    	double totalOutValue = 0;    	
        int    numInputs     = tx.numInputs();
        
        UTXOPool uniqueUtxos = new UTXOPool();
        
        
        for(int i=0; i<numInputs; i++)//Loop over the transaction's inputs
        {
        	Transaction.Input input = tx.getInput(i);
        	if(input == null)
        		continue;
        	
        	UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
        	
        	/* (1) */
        	if(utxoPool.contains(utxo) == false)     //The coin in a specific transaction with this index was used already
        		return false;
        	
        	Transaction.Output output = utxoPool.getTxOutput(utxo);
        	
        	/* (2) */
        	if( (Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature)) == false) //malformed signature
        		return false;
        	
        	/* (3) */
        	if(uniqueUtxos.contains(utxo) == true)//UTXO is claimed twice in the same transaction
        		return false;
        	uniqueUtxos.addUTXO(utxo, output);
        	        	
        	totalInValue += output.value;        	
        }
    		
        for(Transaction.Output out : tx.getOutputs())
        {
        	/* (4) */
        	if(out.value < 0)
        		return false;
        	
        	totalOutValue += out.value;
        }
    	
        /* (5) */
        if(totalInValue < totalOutValue)
        	return false;
        
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) 
    {
    	ArrayList<Transaction> validTransactions = new ArrayList(); 
    	
    	for(Transaction tx : possibleTxs)
    	{
    		if(isValidTx(tx) == true)
    		{
    			validTransactions.add(tx);
    			
    			for(Transaction.Input in:tx.getInputs())
    			{    				
    				UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
    				utxoPool.removeUTXO(utxo);
    			}
    			
    			for(int i=0;i<tx.numOutputs();i++)
    			{
    				Transaction.Output out = tx.getOutput(i);    				
    			}    			
    		}    			
    	}
    	
    	Transaction[] arr = new Transaction[validTransactions.size()];
    	for(int i=0;i<arr.length;i++)
    		arr[i] = validTransactions.get(i);
    	
    	return arr;        
    }
    
    

}
