import java.util.ArrayList;
import java.util.Arrays;

public class TxHandler {

    private UTXOPool utxoPoolCopy;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        utxoPoolCopy = new UTXOPool(utxoPool);
    }

    private boolean checkIfClaimedMultipleTimes(Transaction tx) {
        //get all the output transactions from tx
        ArrayList<Transaction.Input> transactionInputs= tx.getInputs();
        ArrayList<Transaction.Output> claimedTransactionOutputs = new ArrayList<Transaction.Output>();
        //for each output, create a UTXO..then try to get the transaction output from the
        //mapping in UTXOPool - if null allOutputsPresent = false;
        for (int i = 0; i < transactionInputs.size(); i++ ) {
            Transaction.Input currentTransactionInput = transactionInputs.get(i);
            UTXO newUtxo = new UTXO(currentTransactionInput.prevTxHash, currentTransactionInput.outputIndex);
            Transaction.Output newTransactionOutput = utxoPoolCopy.getTxOutput(newUtxo);
            if (claimedTransactionOutputs.contains(newTransactionOutput) ) {
                return true;
            } else {
                claimedTransactionOutputs.add(newTransactionOutput);
            }
        }
        return false;
    }

    private boolean allOutputValuesAreNonNegative(Transaction tx) {
        ArrayList<Transaction.Output> transactionOutputs = tx.getOutputs();
        for (int i=0; i < transactionOutputs.size(); i++) {
            if ( transactionOutputs.get(i).value < 0) {
                return false;
            }
        }
        return true;
    }

    private boolean checkIfAllOutputsInUTXOPool(Transaction tx) {

        //get all the output transactions from tx
        ArrayList<Transaction.Input> transactionInputs= tx.getInputs();
        //for each output, create a UTXO..then try to get the transaction output from the
        //mapping in UTXOPool - if null allOutputsPresent = false;
        for (int i = 0; i < transactionInputs.size(); i++ ) {
            Transaction.Input currentTransactionInput = transactionInputs.get(i);
            UTXO newUtxo = new UTXO(currentTransactionInput.prevTxHash, currentTransactionInput.outputIndex);
            Transaction.Output newTransactionOutput = utxoPoolCopy.getTxOutput(newUtxo);
            if (newTransactionOutput == null) {
                return false;
            }
        }
        return true;
    }

    private boolean inputValuesGreaterThanOrEqualToOutputValues(Transaction tx) {
        ArrayList<Transaction.Input> transactionInputs = tx.getInputs();
        ArrayList<Transaction.Output> transactionOutputs = tx.getOutputs();

        double totalInputValue = 0;

        for (int i = 0; i < transactionInputs.size(); i++) {
            Transaction.Input currentTransactionInput = transactionInputs.get(i);
            UTXO newUtxo = new UTXO(currentTransactionInput.prevTxHash, currentTransactionInput.outputIndex);
            Transaction.Output newTransactionOutput = utxoPoolCopy.getTxOutput(newUtxo);
            totalInputValue += newTransactionOutput.value;
        }

        double totalOutputValue = 0;

        for (int j = 0; j < transactionOutputs.size(); j++) {
            Transaction.Output currentTransactionOutput = transactionOutputs.get(j);
            totalOutputValue += currentTransactionOutput.value;
        }

        if ( totalInputValue < totalOutputValue) {
            return false;
        } else {
            return true;
        }
    }

    private boolean checkAllInputSignatures(Transaction tx) {
        ArrayList<Transaction.Input> transactionInputs = tx.getInputs();

        for (int i = 0; i < transactionInputs.size(); i++ ) {
            Transaction.Input currentTransactionInput = transactionInputs.get(i);
            UTXO newUtxo = new UTXO(currentTransactionInput.prevTxHash, currentTransactionInput.outputIndex);
            Transaction.Output previousTransactionOutput = utxoPoolCopy.getTxOutput(newUtxo);

            if(previousTransactionOutput == null) {
                return false;
            }
            if (!Crypto.verifySignature(previousTransactionOutput.address, tx.getRawDataToSign(i), currentTransactionInput.signature)) {
                return false;
            }
        }
        return true;
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
    public boolean isValidTx(Transaction tx) {

        if (tx == null) {
            return false;
        }

        if (!this.checkIfAllOutputsInUTXOPool(tx)) {
            return false;
        }

        if (!this.checkAllInputSignatures(tx)) {
            return false;
        }

        if (this.checkIfClaimedMultipleTimes(tx)) {
            return false;
        }

        if(!this.allOutputValuesAreNonNegative(tx)) {
            return false;
        }

        if (!this.inputValuesGreaterThanOrEqualToOutputValues(tx)) {
            return false;
        }

        return true;
    }

    private void removeUTXOFromPool(ArrayList<Transaction.Input> transactionInputs) {
        for (int i = 0; i < transactionInputs.size(); i++) {
            Transaction.Input currentTransactionInput = transactionInputs.get(i);
            UTXO newUtxo = new UTXO(currentTransactionInput.prevTxHash, currentTransactionInput.outputIndex);
            utxoPoolCopy.removeUTXO(newUtxo);
        }
    }

    private void addUTXOToPool(Transaction tx) {
        ArrayList<Transaction.Output> transactionOutputs = tx.getOutputs();
        for (int i = 0; i < transactionOutputs.size(); i++) {
            Transaction.Output currentTransactionOutput = transactionOutputs.get(i);
            UTXO newUtxo = new UTXO(tx.getHash(), i);
            utxoPoolCopy.addUTXO(newUtxo, currentTransactionOutput);
        }
    }
    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> mutuallyValidTransactions = new ArrayList<Transaction>();

        for (int i = 0; i < possibleTxs.length; i++ ) {
            if (isValidTx(possibleTxs[i])) {
                mutuallyValidTransactions.add(possibleTxs[i]);
                removeUTXOFromPool(possibleTxs[i].getInputs());
                addUTXOToPool(possibleTxs[i]);
            }
        }

        return mutuallyValidTransactions.toArray(new Transaction[mutuallyValidTransactions.size()]);
    }

}
