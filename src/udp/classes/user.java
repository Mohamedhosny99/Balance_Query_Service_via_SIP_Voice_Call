
package udp.classes;


class user {
    private String msisdn;
    private int balance;
  

    public user(String msisdn, int balance) {
        this.msisdn = msisdn;
        this.balance = balance;
      
         
    }

    public String getMsisdn() {
        return msisdn;
    }
    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }
    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

}