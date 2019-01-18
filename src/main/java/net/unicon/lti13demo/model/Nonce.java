package net.unicon.lti13demo.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;


@Entity
@Table(name = "nonces")
public class Nonce {

    @Id
    @Column(name = "nonce", nullable = false)
    private String nonce;

    public Nonce() {
        this.nonce = UUID.randomUUID().toString();
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }
}
