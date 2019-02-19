/**
 * Copyright 2019 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.unicon.lti13demo.database;

import net.unicon.lti13demo.config.ApplicationConfig;
import net.unicon.lti13demo.model.LtiUserEntity;
import net.unicon.lti13demo.model.PlatformDeployment;
import net.unicon.lti13demo.model.RSAKeyEntity;
import net.unicon.lti13demo.repository.LtiUserRepository;
import net.unicon.lti13demo.repository.PlatformDeploymentRepository;
import net.unicon.lti13demo.repository.RSAKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Check if the database has initial data in it,
 * if it is empty on startup then we populate it with some initial data
 */
@Component
@Profile("!testing")
// only load this when running the application (not for unit tests which have the 'testing' profile active)
public class DatabasePreload {

    static final Logger log = LoggerFactory.getLogger(DatabasePreload.class);

    @Autowired
    ApplicationConfig applicationConfig;

    @Autowired
    @SuppressWarnings({"SpringJavaAutowiredMembersInspection", "SpringJavaAutowiringInspection"})
    RSAKeyRepository rsaKeyRepository;
    @Autowired
    @SuppressWarnings({"SpringJavaAutowiredMembersInspection", "SpringJavaAutowiringInspection"})
    LtiUserRepository ltiUserRepository;
    @Autowired
    @SuppressWarnings({"SpringJavaAutowiredMembersInspection", "SpringJavaAutowiringInspection"})
    PlatformDeploymentRepository platformDeploymentRepository;

    @Value("${oicd.privatekey}")
    private String ownPrivateKey;
    @Value("${oicd.publickey}")
    private String ownPublicKey;


    @PostConstruct
    public void init() {

        if (platformDeploymentRepository.count() > 0) {
            // done, no preloading
            log.info("INIT - no preload");
        } else {
            // preload the sample data
            log.info("INIT - preloaded keys and user");
            // create our sample key

            rsaKeyRepository.saveAndFlush(new RSAKeyEntity("OWNKEY", true,
                    getOwnPublicKey(),
                    getOwnPrivateKey()));

            PlatformDeployment iss1 = new PlatformDeployment();
            iss1.setClientId("Ddbo123456");
            iss1.setIss("https://sakai.org");
            String iss2PublicKey = "-----BEGIN PUBLIC KEY-----" +
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwuvy1UpBbEzUF0C56CoA" +
                    "m14BuBpUJGrJTTpSLbi4rS0xnUgAohkri9CRexbjpPNjbAYaSi4/171T2eHlfAi4" +
                    "Qsv33jEdWgL8HfqFLqN09rHpxhBqWA8sFTARWgA1k7Ti/VeGclx41asCNxUnv0W+" +
                    "mDeyOBSiox6cyx04LZlxs0MkmGBP1Xf4Saq8wGaBI/lUwY52aGtveMkvH/xN8DNQ" +
                    "dk7Li9Q0tj3MCtpI7LE2c2h95Zl/DndDNrRAdHYgOdZg9EQcfiuWdRtUxufkdMoZ" +
                    "mVoYDo7H96tulDMudC0JB0MvaOnnb+MU9jIVuvQkvrZ0jhGmTx8K0gvz2QAgWw6/" +
                    "mwIDAQAB" +
                    "-----END PUBLIC KEY-----";
            iss1.setOidcEndpoint("https://lti-ri.imsglobal.org/platforms/89/authorizations/new");
            iss1.setDeploymentId("0002");
            iss1.setToolKid("hf8Sisblt0zj0KhjY8oAIH0ylU2PuYwnegc8Y9vJq9g");
            iss1.setPlatformKid("hf8Sisblt0zj0KhjY8oAIH0ylU2PuYwnegc8Y9vJq9g");
            String tool2PrivateString = "-----BEGIN PRIVATE KEY-----" +
                    "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC2QJCkV2gFoQD2" +
                    "z7dQRq7g5qIxPaZJJZAJ07wPxdAJiyuWbo0bMOvH//5IqmOnUdal7iNYtDKwr9Cx" +
                    "6UMqI34q6b080GPypyl058vR7Z31ZNv9d4csp81DJxW9UcSkuqTWbEDRvoHUXJLt" +
                    "mhO+CokQSkS2oM1mWQeV1r3T73zPUsUq/QinK8SgGamFb+TbWQOIbqCymKwnt0no" +
                    "2Vg7bqfelkAWXAIMBo9WoiNDiT7v3Ns7Fu8NJ64stXSYC8zzmTWbiUkx06SbJOKz" +
                    "H0HC9NqZmeOwcDyStXYt6mVJg+bOtpctuikCZIkYeJuEwWkR9LqvAdoCs1kVnq8k" +
                    "F1alNd35AgMBAAECggEALBamZvs2ENaIEyzgnazbtVBVwC+3wE4z8Aymm/Iwh36B" +
                    "Rtzrib5l63YEH7QIc3uav31CU70T3iZKCB/zvYfkh6EPxFxtMVA6+Srx5ZDj+28w" +
                    "wLpfmu/k+e/ElI3pUihMpAqAC71YTvUuHgh96iVGTwiIYt23kqDK8vaF6XUv7j8h" +
                    "D1do+4eX9oZM03dqh2cZfC1z+xdhiEQzEOSu7qcNhml6d/rpS0EkILnmBekA1adw" +
                    "UuaS/FQzcbggScSGtL2WL6CFB1gl82IGhJALqRASfRGWlkmlnTQ1fzYZdLLvWKlG" +
                    "MM1mWu3zmOGxNSKQwpEHlxDpSxemFAf7RkgavA5EeQKBgQDihvyG1Ba9xtW9jO80" +
                    "BPCpvyCmpX0SlhlP7kYKtZHqkEKd+SOvfzN8fxi/5BNRXnMmJFN3Mkc2sYssMzTx" +
                    "MABii2e6r02AwkLUBu2DX5O/qauCbVlhr1LtvMbKTw6jnJYpGkZMqnTTS/933DPD" +
                    "8xa8AsckFMsXiGRs9OpFpOF+cwKBgQDN9uUVbarh3o6xx4pABNp3QDLQeqllUlsr" +
                    "Z4JqX26MELE1hA5qaccaLMtSY5Pq8Qh36tQJhZFAYz3isxvEhhIkAZZKmKi9MKDK" +
                    "lf+u7vYWfpNYxUPwpB9ZRM4UCcquY24/FgKucorQI0KwYqOTJX2whKDBjiurINA2" +
                    "x658s5TK4wKBgAQqQThla+mfX0y166wELzyfxATsZAlUczCyC92kiwNKFb971jki" +
                    "2JqAZ78XfXdwiiN4ZYR6iy6pQwrUAjQxEsC9GXIoSP+GEt59Jh7VQg0zHHEwe4U9" +
                    "SQQQBYOwwm8lsOkej45XUACWlCLrDJScwp1AW9MBAt7y5g3OzwPqzS6bAoGAFoVO" +
                    "mz84liX9uFa3OTTOpodwhvdCmn+c1GwnCHaS4eHZXp6n7N7QFH6dZM7al6/vWx1k" +
                    "Pf5K2Z2AYM9w09ZNGX7K7jEvEjDFBCHOqVQbuG3yspwvR5rKirpJRkujy9m3blJ7" +
                    "zJNdtlCEtEC03hwVWD3ITiG7iKS336WJ4LzKIj0CgYBhhcvs9rnEx0pbMPyw3eK+" +
                    "v2utJ02u3MsWmynJbvjqTSwZhRfBlDA2uzOLvPUNNOWiGjExCrAe+fFkuO8l72wu" +
                    "T8RzsVTPwN9uKZOlm/sHd7KtETaMXRM94mT/uisQ9QahX48tw/c4miu+Sv2xWwQ1" +
                    "sNJ4OXzO/tir0uLgMp6XcA==" +
                    "-----END PRIVATE KEY-----";
            String tool2PublicString ="-----BEGIN PUBLIC KEY-----" +
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtkCQpFdoBaEA9s+3UEau" +
                    "4OaiMT2mSSWQCdO8D8XQCYsrlm6NGzDrx//+SKpjp1HWpe4jWLQysK/QselDKiN+" +
                    "Kum9PNBj8qcpdOfL0e2d9WTb/XeHLKfNQycVvVHEpLqk1mxA0b6B1FyS7ZoTvgqJ" +
                    "EEpEtqDNZlkHlda90+98z1LFKv0IpyvEoBmphW/k21kDiG6gspisJ7dJ6NlYO26n" +
                    "3pZAFlwCDAaPVqIjQ4k+79zbOxbvDSeuLLV0mAvM85k1m4lJMdOkmyTisx9BwvTa" +
                    "mZnjsHA8krV2LeplSYPmzraXLbopAmSJGHibhMFpEfS6rwHaArNZFZ6vJBdWpTXd" +
                    "+QIDAQAB" +
                    "-----END PUBLIC KEY-----";
            rsaKeyRepository.saveAndFlush(new RSAKeyEntity("hf8Sisblt0zj0KhjY8oAIH0ylU2PuYwnegc8Y9vJq9g",true, tool2PublicString,tool2PrivateString));
            rsaKeyRepository.saveAndFlush(new RSAKeyEntity("hf8Sisblt0zj0KhjY8oAIH0ylU2PuYwnegc8Y9vJq9g",false, iss2PublicKey,null));

            platformDeploymentRepository.saveAndFlush(iss1);

            PlatformDeployment iss2 = new PlatformDeployment();
            iss2.setClientId("imstestuser");
            iss2.setIss("ltiadvantagevalidator.imsglobal.org");
            iss2.setJwksEndpoint("https://oauth2server.imsglobal.org/jwks");
            String iss3PublicKey = "";
            iss2.setOidcEndpoint("https://ltiadvantagevalidator.imsglobal.org/ltitool/oidcauthurl.html");
            iss2.setoAuth2TokenUrl("https://oauth2server.imsglobal.org/oauth2server/authcodejwt");
            iss2.setDeploymentId("testdeploy");
            iss2.setToolKid("imstester_4");
            iss2.setPlatformKid("imstester_4");
            String tool3PrivateString = "-----BEGIN RSA PRIVATE KEY-----" +
                    "MIIEowIBAAKCAQEAsW3eobPIj5LsyHcMGckVSSC621uL+0zkeMoWfXfNmvTH+zt5" +
                    "WOeEIdz+X7fK+F+lO7ic5WdJEGmp9/cjAf0Z6SsmnvvHlHV/xsWtJm4DiuuF2MAa" +
                    "hRQ5QEkhaEdh5QM2vAYyc8Nfxe504vA3czuynrW9MsOdZHeVzF+zWhhEl+olC5fW" +
                    "A1rhTUPpdxuZ0opVIrGJtI/QYfndoN+7zTs/4CXqG6WpB+AZio8j7c6fJLC7J33c" +
                    "pxB1+O+64Qbh+5sxz46cEByboAB8qerYCmcfxxfBbwyySBBK5X77aNHWA01B1kpO" +
                    "Q2VB8YKQk+OrXsPgJobPkR9ONWa9DC9JjEdUJwIDAQABAoIBAQCAA+qutt2NIY/v" +
                    "71zuudO+yHupSzsLXOY3dG+XpTnWhKhJTxb1m00Ndbqe6yfp3nCET2X8anIgAmzc" +
                    "+RXsGGZ6gmTCLp1IMyK3EuckJBowQFB5G9nGjNnl1R3idCZgqtnx/XKnbZ6LW8o/" +
                    "9tu7K6ZrtmrE1riXxWRyadYoufu7ssNTqtj03oh3Tvw+Ze6xvF6hpaxnbVHxJcGt" +
                    "xZO51L6rGOSFq5CJ81BswyBDOKB/Z2OC0o3m2t4ZF4/2Lf070sB7RoejGD7mhYVe" +
                    "lEOoC95C14hfcspzmDEb8I/n0MvAxlwddM4KZRilAJ+e2R0rM9M1MnyYsmYUsMNX" +
                    "EKWcx+/5AoGBAOLtNVbIohpY5kbX4WREJ/0INPbbx0gf68ozEZTjsOzIP7oaIzry" +
                    "URmxyZzSpx446QCO8s26vuxrPGm7OAteNS7UpDdunzKsaIlZScZQEpE9htp3MKKw" +
                    "KXaA4l7H55uWWnaUAcDqjEdybhYL6SbPKhOaK53VeHOLro900FiRnfaDAoGBAMgp" +
                    "O8GwAI3LbD06Fn+DT+3hj/i8wxbWilgJlI+RU+wWfQ421jMKv2dck8zbnzKGxEwA" +
                    "3WPh6gGMlkavEZ95d0qZ/TOkSh+VIjJuOrjcckRcrKcycYJJUzreO7ENsFbA+8xL" +
                    "Qp2gNV+NntiChzSUGY5Nup3keoaT9iV13oYDSdqNAoGARDn9Z3I7CqDf2zzcz0CO" +
                    "pUzqX64EZHL0eX6RMqqibw5l2pYxMW/ZYlhJvZS4GiYSJ9DSv3f+Hya+qytW1lQk" +
                    "uUfFd8USqDGd3G2z+KPqcTCGcviS7tb4IGDvrn976xNxb2VggZgDRRfqcUZzeu+e" +
                    "PvaDVpjv9g1xFkCQw5BEZfECgYBcSB5jywhGV14c0FYlDd5g9xiQfj6XnewEcM5M" +
                    "bp05gJjBX+jbeX4LYnRGA49fFSEVRWTMsxBXDIEQL5C5bJ/iBiLllz4RV4l/pLBw" +
                    "IDqSaAO1xhztC29S+bidhYkiRjEQ3DXnREC3QCzW9z7sr8ckg5OhTgBrYXYfiTtB" +
                    "n+yB1QKBgG/J+WhkqMEtZ8CgdoiTIqYKmFsLvl07wETAVU6Nv1sEI+jnhyug0QtQ" +
                    "yLAlBOVyrXuJ1DZMX6hTRij4L0jvnJFSq0Sv8COuLIH90xdq/NTNQ3LAy60l/3b1" +
                    "ojAnnRJORDegdJjCBxJ59Fch6Qfd+e8742DVsJu8zVo2garUVMH3" +
                    "-----END RSA PRIVATE KEY-----";
            String tool3PublicKey ="";
            rsaKeyRepository.saveAndFlush(new RSAKeyEntity("imstester_4",true, tool3PublicKey,tool3PrivateString));
            rsaKeyRepository.saveAndFlush(new RSAKeyEntity("imstester_4",false, iss3PublicKey,null));
            platformDeploymentRepository.saveAndFlush(iss2);


            PlatformDeployment iss5 = new PlatformDeployment();
            iss5.setClientId("e22e7d54-7280-4739-9575-9f7381125827");
            //54e11a50-56ea-4fb8-935f-f3adb25bc60f
            //pPn1MIWbhs3UZXzyp1gfKebc2u5gAJrW
            iss5.setIss("https://blackboard.com");
            String iss5PublicKey = "";
            iss5.setJwksEndpoint("https://devportal-playground.saas.bbpd.io/api/v1/management/applications/e22e7d54-7280-4739-9575-9f7381125827/jwks.json");
            iss5.setoAuth2TokenUrl("https://devportal-playground.saas.bbpd.io/api/v1/gateway/oauth2/jwttoken");
            iss5.setOidcEndpoint("https://devportal-playground.saas.bbpd.io/api/v1/gateway/oauth2/jwttoken");
            iss5.setDeploymentId("f51fea82-9cdc-4f11-8b91-9f3106bd7df9");
            iss5.setToolKid("e22e7d54-7280-4739-9575-9f7381125827");
            iss5.setPlatformKid("e22e7d54-7280-4739-9575-9f7381125827");
            String tool5PrivateString = "-----BEGIN PRIVATE KEY-----" +
                    "MIIEwAIBADANBgkqhkiG9w0BAQEFAASCBKowggSmAgEAAoIBAQCSETeesKmwy7Je" +
                    "dxc0+4hi6Z9jH4vanI6VCUkoSeRVFu+W1OxYuJ8Lp8V6arNoNVM7NpFKlb/B32z2" +
                    "L1nbU2TupP2AhwJzJiBZyr2h9JrjCwO1CjrTxCARy0f2pVaNJd1cFsEy5ciqTDrr" +
                    "+ew+GdGAyrW3lTvbUYJ9YiisLuDYLEz63ysQizXVJM6yP0w/L0LzQjV7Y1GUqgOi" +
                    "4VO3cVDlYA9CnrXx/+A36eUoLlWEG4kqrxX6z6sYx5wvDkbZNsXW+NharwpCID4x" +
                    "WDgHD+b147y3vUa5rhCMotplJCG4xe176nCW9cI5b1NnEdhoxz9YmqE73HRkbWCi" +
                    "dWJ6jMfXAgMBAAECggEBAIaeN5SZXMsD8K//MfQyndALYEoKmOoxv3a7yCDJeRay" +
                    "vL1WHzzlai9jwCzCifZQrxSzhdQnj0Mul02M9lOc2DjY9omt5CQbz1Tx9TN+LAFz" +
                    "9Ua01uK0rpm3r+sxFkOf0hM9JXGzNO0+tE8nVwMk6GC6ch9v+mp3BLxp6vJFghO3" +
                    "1wkC33y5uF9vK0nQJE2J4R3qD397Q1NtWC6UK7WVpqa6Hmobas0QMxc4kwC+YCL0" +
                    "FmfbyQ8ljvqrkd7DtxOSodWKrvyhZ8+u/JWTpRKP0v2KDdj4FnuzDGT5Xu/u4QKV" +
                    "Auh1znuILCr4MrSnFTtx95icYWTVMK6EOOX+McCefgECgYEA5JzXCcFtvO+ocMA6" +
                    "WINenQnqg+w4mPzkWbyRI2WGUG3ZmUOFzkJBrjiADY0QOPaxV94M/aU2FfUtAggn" +
                    "g/EC1ldWv/aCHyrwukVMkXRqIjSrovJs50F99OVOkBkuZ1DN+kBj8bVdm4czuON3" +
                    "5EQL4jvkAo2I0q2+UEqlm2ZgVQcCgYEAo5DZxtiaoucU1iR+JQtv4eT0felzRVBK" +
                    "8Brijop4NSyWPWvUhvJtBUbF0sBo6EMBG68F2K2PsIkCgjzIJvhv7bNj9Tt8LLr/" +
                    "LenxOcWfkuwXKY8Jgk9kFmXdH5oXbZDnLTmFlalHsMDcvafF2wqXv4i5oQyg/Bg/" +
                    "i5YrxOnskrECgYEAvjBlDa6oFvjijvq9D4orGkZ3nczVwSES8DzaGjKKLTtWPhVY" +
                    "qiGg9sn2bk+e4WUKYOlQsWvStS3Fhd020qOdEqiSeR0hX8CfiLfCXXgWG+Lpr6Qz" +
                    "SajImwZx95ubaURmN/19qEaUW6F9PCGxSmv53vqoVB9fZrY4kB+p6sCe/90CgYEA" +
                    "kDN567a2zfuQASL3G02z+6FQc1lHSMp65qetKSGmzKpcYhYYrkg4GTjF2wHK+0ln" +
                    "RrLNIrQRQ//meE+B3emVNCi9bUxeM0lBA4eGJfq2rl76JKd4KKw5fw2bHutI0E25" +
                    "ocNepA0zqP6/HW02H+B83F172iI6I0Z1b2ibeeUJrmECgYEAwoVdi9jFRtKSw78T" +
                    "fCSDgNELCWudY16zEHU1vfTjJddJNid8tk+5t2iVFgyTN6lvdIUB9VeVNjm3/JfZ" +
                    "rAnWMIQobm6A51YH7Wu9vN6q5cWrpF2EEop869iY+swWPcM7H+wygnMveWHsujyw" +
                    "4JuZ6t+pC9spPhpn3GtEn9YbMCE=" +
                    "-----END PRIVATE KEY-----";
            String tool5PublicString ="";
            rsaKeyRepository.saveAndFlush(new RSAKeyEntity("ec03af85-659a-4d2f-b5b0-79e9c5f8b0e7",true, tool5PublicString,tool5PrivateString));
            rsaKeyRepository.saveAndFlush(new RSAKeyEntity("ec03af85-659a-4d2f-b5b0-79e9c5f8b0e7",false, iss5PublicKey,null));

            platformDeploymentRepository.saveAndFlush(iss5);


            PlatformDeployment iss6 = new PlatformDeployment();
            iss6.setClientId("97140000000000039");
            iss6.setIss("https://canvas.instructure.com");
            String iss6PublicKey = "";
            iss6.setJwksEndpoint("https://unicon.beta.instructure.com/api/lti/security/jwks");
            iss6.setoAuth2TokenUrl("https://unicon.beta.instructure.com/api/lti/authorize");
            iss6.setOidcEndpoint("https://unicon.beta.instructure.com/api/lti/authorize");
            iss6.setDeploymentId("129:abe5b442f7a9c8b74990d5e9fddaa4eb7d662850");
            iss6.setToolKid("97140000000000039");
            iss6.setPlatformKid("97140000000000039");
            String tool6PrivateString = "-----BEGIN PRIVATE KEY-----" +
                    "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC2QJCkV2gFoQD2" +
                    "z7dQRq7g5qIxPaZJJZAJ07wPxdAJiyuWbo0bMOvH//5IqmOnUdal7iNYtDKwr9Cx" +
                    "6UMqI34q6b080GPypyl058vR7Z31ZNv9d4csp81DJxW9UcSkuqTWbEDRvoHUXJLt" +
                    "mhO+CokQSkS2oM1mWQeV1r3T73zPUsUq/QinK8SgGamFb+TbWQOIbqCymKwnt0no" +
                    "2Vg7bqfelkAWXAIMBo9WoiNDiT7v3Ns7Fu8NJ64stXSYC8zzmTWbiUkx06SbJOKz" +
                    "H0HC9NqZmeOwcDyStXYt6mVJg+bOtpctuikCZIkYeJuEwWkR9LqvAdoCs1kVnq8k" +
                    "F1alNd35AgMBAAECggEALBamZvs2ENaIEyzgnazbtVBVwC+3wE4z8Aymm/Iwh36B" +
                    "Rtzrib5l63YEH7QIc3uav31CU70T3iZKCB/zvYfkh6EPxFxtMVA6+Srx5ZDj+28w" +
                    "wLpfmu/k+e/ElI3pUihMpAqAC71YTvUuHgh96iVGTwiIYt23kqDK8vaF6XUv7j8h" +
                    "D1do+4eX9oZM03dqh2cZfC1z+xdhiEQzEOSu7qcNhml6d/rpS0EkILnmBekA1adw" +
                    "UuaS/FQzcbggScSGtL2WL6CFB1gl82IGhJALqRASfRGWlkmlnTQ1fzYZdLLvWKlG" +
                    "MM1mWu3zmOGxNSKQwpEHlxDpSxemFAf7RkgavA5EeQKBgQDihvyG1Ba9xtW9jO80" +
                    "BPCpvyCmpX0SlhlP7kYKtZHqkEKd+SOvfzN8fxi/5BNRXnMmJFN3Mkc2sYssMzTx" +
                    "MABii2e6r02AwkLUBu2DX5O/qauCbVlhr1LtvMbKTw6jnJYpGkZMqnTTS/933DPD" +
                    "8xa8AsckFMsXiGRs9OpFpOF+cwKBgQDN9uUVbarh3o6xx4pABNp3QDLQeqllUlsr" +
                    "Z4JqX26MELE1hA5qaccaLMtSY5Pq8Qh36tQJhZFAYz3isxvEhhIkAZZKmKi9MKDK" +
                    "lf+u7vYWfpNYxUPwpB9ZRM4UCcquY24/FgKucorQI0KwYqOTJX2whKDBjiurINA2" +
                    "x658s5TK4wKBgAQqQThla+mfX0y166wELzyfxATsZAlUczCyC92kiwNKFb971jki" +
                    "2JqAZ78XfXdwiiN4ZYR6iy6pQwrUAjQxEsC9GXIoSP+GEt59Jh7VQg0zHHEwe4U9" +
                    "SQQQBYOwwm8lsOkej45XUACWlCLrDJScwp1AW9MBAt7y5g3OzwPqzS6bAoGAFoVO" +
                    "mz84liX9uFa3OTTOpodwhvdCmn+c1GwnCHaS4eHZXp6n7N7QFH6dZM7al6/vWx1k" +
                    "Pf5K2Z2AYM9w09ZNGX7K7jEvEjDFBCHOqVQbuG3yspwvR5rKirpJRkujy9m3blJ7" +
                    "zJNdtlCEtEC03hwVWD3ITiG7iKS336WJ4LzKIj0CgYBhhcvs9rnEx0pbMPyw3eK+" +
                    "v2utJ02u3MsWmynJbvjqTSwZhRfBlDA2uzOLvPUNNOWiGjExCrAe+fFkuO8l72wu" +
                    "T8RzsVTPwN9uKZOlm/sHd7KtETaMXRM94mT/uisQ9QahX48tw/c4miu+Sv2xWwQ1" +
                    "sNJ4OXzO/tir0uLgMp6XcA==" +
                    "-----END PRIVATE KEY-----";
            String tool6PublicString ="";
            rsaKeyRepository.saveAndFlush(new RSAKeyEntity("97140000000000039",true, tool6PublicString,tool6PrivateString));
            rsaKeyRepository.saveAndFlush(new RSAKeyEntity("97140000000000039",false, iss6PublicKey,null));

            platformDeploymentRepository.saveAndFlush(iss6);


            PlatformDeployment iss7 = new PlatformDeployment();
            iss7.setClientId("0722ac4c-70ee-4e5c-bc28-6c825e6e339e");
            iss7.setIss("https://trunk-mysql.nightly.sakaiproject.org");
            String iss7PublicKey = "-----BEGIN PUBLIC KEY-----" +
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAq2oxpAXTjgAdOPPJJaZ/K" +
                    "eOUkYZ5SuazWfF3FLT7rTx2JGvGwOuEzLjxkZRfSS6+3EvEGVTy6riP4X7WAFYXFZ" +
                    "KTw5vNyRCxZgKSAMgwl2fznoyF8eDMCuXbmIQmCC9Xk6wjAL7ZzKPpLtRDzc0vk+2" +
                    "BRRqLBIAWFoKwqQhRtO/ZxVLyPq4tt4oGboNn9zYcN4csVeU9bRPq6rfbL4HFOjA6" +
                    "pESsvcyjPoOfFdWSOTdzn4PB5fWdVaJnuHHvZMRF4p+NY9jk43GAYqkURb+zRQsmD" +
                    "q8Q3l1c1+nvFYRU8rnSOuPVg1PMzpPrlNTXVqsl0M4vY46HiIDsx7BBAd/YYwIDAQ" +
                    "AB" +
                    "-----END PUBLIC KEY-----";
            iss7.setJwksEndpoint("");
            iss7.setoAuth2TokenUrl("https://trunk-mysql.nightly.sakaiproject.org/imsoidc/lti13/oidc_auth");
            iss7.setOidcEndpoint("https://trunk-mysql.nightly.sakaiproject.org/imsoidc/lti13/oidc_auth");
            iss7.setDeploymentId("1");
            iss7.setToolKid("472224775");
            iss7.setPlatformKid("472224775");
            String tool7PrivateString = "-----BEGIN PRIVATE KEY-----" +
                    "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCGr8USdDyOnpWC/" +
                    "Eaih4vRFQG0XzgxtnaPv1pFeitfZ9KzPAnmWL55JNCSpEe02TpeNBQs+hpkKFkp1m" +
                    "J0kGk3WSbMaVQ074cQ+5eD3darIkIvwzxe9tO4QPGCzbUWoFr7S2l40+XuDEK8+I9" +
                    "Q+oFW0+GFb1Iime55mk2ibF2KL9kmc6QvMGiDfGHyYTUOxgMH+FNgfVuTboMSNn4u" +
                    "keujUjNAcXECbfh1XfXKRh2bgXWacSaQx7/rfPNFHhgmdbPbVrhoOZ0hDSYDL7vyg" +
                    "9WDS0tiwLiyNZ9jPjtivUFYYrnSWTaKKbD6jkH9EfZAJbzzCHt5hnFbVM/2jnYmiu" +
                    "h9AgMBAAECggEACtlXt3/TbO2LP4zQp8DhW0sk4wGUhjSbYkLYo6YOnlW/nB5m8b4" +
                    "OQbXccyN2fzPMRtBNG75dp9m0LPGO5rLHNuUggfCuDsGIkK6jasTQyOIukKhaUY/y" +
                    "R/I7Qf0N7C+ohHtzmH8BE2a7bXtcF/rFtlfE37my1QSvN+5c+ItgEYRgOcn+emAm+" +
                    "JIO3H5V+8R8oLVxdHj1YPmdCQUUuHf7JJr4ebQaivv/hXrbn/Fc5f/iUqyWKotAA0" +
                    "3ElC1IgWpTCI5CiALqz7c/EKEYleUljOVvAHXI5abAzuSZ6agnpzsgBFhAtKjDiy6" +
                    "hrxfWhGYnNSjvW0Alc39Zyd72sw/noQKBgQDxF6jHL8hrSEu89rXGn5ZZ3gNT4PVE" +
                    "/mibOzze0TZ9jh/9G8kGyAxZ/SiseZ7wLG26sGpWj6zSMBI8JHLWg+hNfcyUayNw6" +
                    "NJey+fi4+HI02IpK83YSC2MecJiSRYCc9ARbekGJznlQ6Azj/UJWMh4Xa1SbOlTyq" +
                    "IDGcviIGtNBQKBgQCPA8YMpj5dT7ObHK89QaCh+i/copKeXl7kRNtf9NGIJOcXH3D" +
                    "gmQPz2CjRfvcm3p+3fhVR6ds6neUF4nUzcNqtdqUyLyjBKaFOIeMRHd+EtTKCk9M9" +
                    "vnoDN5Z7DsmQfVb/g2mwOJQh17QQYRwSR/r9VJwRnh60vQgWXlkf4SRHGQKBgQC2o" +
                    "GFtYTjEa6NTpN7VcpYFxTNoGoQwmzbKP/lAH+LTARIiPSXuSSNyF170J7zJ2h3UyU" +
                    "YEBBm5rkhh3opPDOSvrpeF5hKWuQaCYJtrN4wVAGrfHRt0tlgqjSE+KnbNrMT0Lvn" +
                    "Re21TygJUjLvlNKfG5c8hHPcyTdCj4KqcxEoDsQKBgCg5dn3eUFIsdabeBYfkKosE" +
                    "dGvJKhXzExt/JnixwjFZt8mwJS52uz7dgHWUEW3UVfXlN5fnf6rX+C+Uh0RlrIv3f" +
                    "/VCgGL8E4uHSndpAJIx6LgDXfnS1jyLaP7JpPXH2y1Yx+QxtbBjHBkzRxL5PeX84z" +
                    "ytTZOmS10HQmgOCgcZAoGADj2RjfHDfoqGVXPcDWOdl0bvUs5QT/2z0DUMEN7f3Jk" +
                    "wibsyCOvVO97kyE+YoD/JTBypt7MlKx+VZRsZmsSFs3sPdH5ODFkkfMegluLdqsCc" +
                    "VGIoO+I+Fqr/cFOq6mllHHeFdbzkHNi/EcLH5S6Qvc+sh2eK/DFDkq39a8MhIHg=" +
                    "-----END PRIVATE KEY-----";
            String tool7PublicString ="-----BEGIN PUBLIC KEY-----" +
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhq/FEnQ8jp6VgvxGooeL0" +
                    "RUBtF84MbZ2j79aRXorX2fSszwJ5li+eSTQkqRHtNk6XjQULPoaZChZKdZidJBpN1" +
                    "kmzGlUNO+HEPuXg93WqyJCL8M8XvbTuEDxgs21FqBa+0tpeNPl7gxCvPiPUPqBVtP" +
                    "hhW9SIpnueZpNomxdii/ZJnOkLzBog3xh8mE1DsYDB/hTYH1bk26DEjZ+LpHro1Iz" +
                    "QHFxAm34dV31ykYdm4F1mnEmkMe/63zzRR4YJnWz21a4aDmdIQ0mAy+78oPVg0tLY" +
                    "sC4sjWfYz47Yr1BWGK50lk2iimw+o5B/RH2QCW88wh7eYZxW1TP9o52JorofQIDAQ" +
                    "AB" +
                    "-----END PUBLIC KEY-----";
            rsaKeyRepository.saveAndFlush(new RSAKeyEntity("472224775",true, tool7PublicString,tool7PrivateString));
            rsaKeyRepository.saveAndFlush(new RSAKeyEntity("472224775",false, iss7PublicKey,null));

            platformDeploymentRepository.saveAndFlush(iss7);

            // create our sample user
            LtiUserEntity user = ltiUserRepository.saveAndFlush(new LtiUserEntity("azeckoski", null));
            ltiUserRepository.saveAndFlush(user);
        }
    }

    public String getOwnPrivateKey() {
        return ownPrivateKey;
    }

    public void setOwnPrivateKey(String ownPrivateKey) {
        this.ownPrivateKey = ownPrivateKey;
    }

    public String getOwnPublicKey() {
        return ownPublicKey;
    }

    public void setOwnPublicKey(String ownPublicKey) {
        this.ownPublicKey = ownPublicKey;
    }
}
