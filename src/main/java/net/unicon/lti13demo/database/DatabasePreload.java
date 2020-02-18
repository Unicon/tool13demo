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
            iss1.setIss("https://lti-ri.imsglobal.org/platforms/89/authorizations/new");
            iss1.setoAuth2TokenUrl("https://lti-ri.imsglobal.org/platforms/281/access_tokens");
            iss1.setJwksEndpoint("https://lti-ri.imsglobal.org/platforms/281/platform_keys/736.json");
            String iss2PublicKey = "-----BEGIN PUBLIC KEY-----" +
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAycS0mTNqPHDF9oiuuhnL" +
                    "4jt7qE3YGh70GW8xipQoI4aO3TZTe/jdzRz3wgS12x+FZ1b1mENVQG+Og6r1jOuO" +
                    "QyNwH8UTvilUBTbxGVbXFLhQNM9hKuPfuEnzE0ALv4nMFXY6961bp2CvrwrI54bk" +
                    "3VyBWDFB1o2Y4jtwtOMBoMpzunxWiaJLJRa1HAtVt0ZqFFFtlYfpZiUok3PEAhxE" +
                    "EDvINjhzVkuYbSDjJ/HGm5PcE1HxlMKRzWT4px6vwmKyrAKnsK2mGtnBc3RwT3RS" +
                    "gJvt0jKhBfkAGhv0J/+e2rVdSvnFMk7A+FvYzgQxmpOHiDDwSUdpcUyT49f6mHsw" +
                    "AQIDAQAB" +
                    "-----END PUBLIC KEY-----";
            iss1.setOidcEndpoint("https://lti-ri.imsglobal.org/platforms/281/authorizations/new");
            iss1.setDeploymentId("0002");
            iss1.setToolKid("hf8Sisblt0zj0KhjY8oAIH0ylU2PuYwnegc8Y9vJq9g");
            iss1.setPlatformKid("hf8Sisblt0zj0KhjY8oAIH0ylU2PuYwnegc8Y9vJq9g");
            String tool2PrivateString = "-----BEGIN RSA PRIVATE KEY-----" +
                    "MIIEpAIBAAKCAQEA1emJEYJebrnPAvrAf6FDCQAOldKF3W+LY8i91L3NvUPgrkKs" +
                    "PjjRO+g0B+sRqKsoWVaN8wZ2j0y+e2YX5+ig1k2bMmNHMgRGISf1rvgMEJA1k9Ri" +
                    "GxWuMeWrP9Aa/nYEs7Wau5dCB0SelGCPHEjrHmHmIzfZGsJG/i1AZ7EKOER90cxQ" +
                    "G3pG8tnQqWNordtxJ7Cqr2/jSAFb5zW++AV9D6xjlSTuk1V3uJbtEH4q2Zid8fA8" +
                    "aAwaNPvL7QbW5IhrZw/chGxD/z3wHb1VQFiyycVjI6LTTmzI4IB9Dkt6QS3jzxft" +
                    "+AkTsJ4250xbCYr2lWsbd1n1+E3uzjipOS5EGQIDAQABAoIBAE0oUneNVbCMtv21" +
                    "IrAmo75gVeJ2sDBiJp4Ub1yIJejJzgYtKGG9LsN0Cyh8Ar+bFQ+8Z7EsOKGRpfdu" +
                    "qNrOjw0dqwguxSRmZEFbX4QAFqH20kyDQ+vPEykOVYnL76CvQxzrOWfGtFtYxfZx" +
                    "Kc2jA6PO3ir/3wCG6QKPofaE5lHKwcpPfacV+1LL7I+NXa1uGzg5aZ2wKH7j2A3R" +
                    "6BJvflwbHFJtDFbicFBtb11ZtHKX6qhMo6Keum/vDglGJam07nPEcCtT+WtPPhF9" +
                    "yKVeMCbXlI9QxcdMyCpQSGt2Sm/8YO5zabnnyZVlws9SBqq5Zd3M520UHC8BQ4aY" +
                    "kjFZ5P0CgYEA8erlhZyYNZysAv4HwynPSAdcY5W1nImQMvGm3NN2cBPs2fevl560" +
                    "DeQn1G+60LGsYRpCl8aVok+mF9rc5yXsl823vB2NQu6/DoT4f753bt6sF6WlqkV5" +
                    "9Cun3DToXxDU9IfcSEge21b4FxMYmwleSmtVfiB8p6n3qDrOoYB+mzMCgYEA4l1M" +
                    "TsXBdJpQ2tU8mIA6HUDlzp7bnlrDWJzoZA+0rMGZZCZZ0D9V0icF3ht7oVMYqGY8" +
                    "ROUDfYJbq7qHbcUOgog8Furs25+iNAM5nlcEsaowdz6kqPxFm2U2Koc0aQSWToPo" +
                    "RLzL6ejggRacXRua5nUwnv8Vx3oKT7eua88fw4MCgYEA1BWZmNRbQI0U5B6u0XNj" +
                    "DIOfhJKoQA79wxvFrM0ahVGCkOirISJ6Ob9vB7fYMMPDGvH5tbPcVQq80ycGCQNf" +
                    "cwpf7OR/hlFmYCVE8kEZ1bITbzvCjA8SxnRLWitsGIPaHnLJNPk9TA/nudr89FZ3" +
                    "Ooj0z3lNr3O78dl0c3QCCq8CgYEA23TaUxBUMqidNNtAqRS/wra1VXEbuFWER7ev" +
                    "cbrsTgRPoxGvRz7wBBMDFEcOv+Og5zpeuehRTu//0ei//YLrQ0+y+gD+axpDlit+" +
                    "Q+1XRquZ2zGnT5FsJnCkZ+y2ug3RbwNhPqrPAtJcPapfI8FslnsNDUh+o+rEbm7E" +
                    "sg6XW+MCgYBDEQpTlxjBg6ulpfMkZ69SCJ00MFrNwVHbkDNMjLmjfYsstqYgJMUT" +
                    "A/TVaslSL07p12NgjKcAQ4I1uoVM9CREiXLXN+RvXr8OfIvgsrpcwZ45/Vni/vLo" +
                    "xDXNkg2Lf9/J5A4d/ibZCuZYoqapdjVIJ5L13Wo540Md7zLDKHyB9w==" +
                    "-----END RSA PRIVATE KEY-----";
            String tool2PublicString ="-----BEGIN PUBLIC KEY-----" +
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1emJEYJebrnPAvrAf6FD" +
                    "CQAOldKF3W+LY8i91L3NvUPgrkKsPjjRO+g0B+sRqKsoWVaN8wZ2j0y+e2YX5+ig" +
                    "1k2bMmNHMgRGISf1rvgMEJA1k9RiGxWuMeWrP9Aa/nYEs7Wau5dCB0SelGCPHEjr" +
                    "HmHmIzfZGsJG/i1AZ7EKOER90cxQG3pG8tnQqWNordtxJ7Cqr2/jSAFb5zW++AV9" +
                    "D6xjlSTuk1V3uJbtEH4q2Zid8fA8aAwaNPvL7QbW5IhrZw/chGxD/z3wHb1VQFiy" +
                    "ycVjI6LTTmzI4IB9Dkt6QS3jzxft+AkTsJ4250xbCYr2lWsbd1n1+E3uzjipOS5E" +
                    "GQIDAQAB" +
                    "-----END PUBLIC KEY-----";
            rsaKeyRepository.saveAndFlush(new RSAKeyEntity("hf8Sisblt0zj0KhjY8oAIH0ylU2PuYwnegc8Y9vJq9g",true, tool2PublicString,tool2PrivateString));
            rsaKeyRepository.saveAndFlush(new RSAKeyEntity("hf8Sisblt0zj0KhjY8oAIH0ylU2PuYwnegc8Y9vJq9g",false, iss2PublicKey,null));

            platformDeploymentRepository.saveAndFlush(iss1);

            PlatformDeployment iss1b = new PlatformDeployment();
            iss1b.setClientId("Ddbo123456");
            iss1b.setIss("https://lti-ri.imsglobal.org/platforms/774");
            iss1b.setoAuth2TokenUrl("https://lti-ri.imsglobal.org/platforms/774/access_tokens");
            iss1b.setJwksEndpoint("https://lti-ri.imsglobal.org/platforms/774/platform_keys/796.json");
            String iss1bPublicKey = "-----BEGIN PUBLIC KEY-----" +
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAycS0mTNqPHDF9oiuuhnL" +
                    "4jt7qE3YGh70GW8xipQoI4aO3TZTe/jdzRz3wgS12x+FZ1b1mENVQG+Og6r1jOuO" +
                    "QyNwH8UTvilUBTbxGVbXFLhQNM9hKuPfuEnzE0ALv4nMFXY6961bp2CvrwrI54bk" +
                    "3VyBWDFB1o2Y4jtwtOMBoMpzunxWiaJLJRa1HAtVt0ZqFFFtlYfpZiUok3PEAhxE" +
                    "EDvINjhzVkuYbSDjJ/HGm5PcE1HxlMKRzWT4px6vwmKyrAKnsK2mGtnBc3RwT3RS" +
                    "gJvt0jKhBfkAGhv0J/+e2rVdSvnFMk7A+FvYzgQxmpOHiDDwSUdpcUyT49f6mHsw" +
                    "AQIDAQAB" +
                    "-----END PUBLIC KEY-----";
            iss1b.setOidcEndpoint("https://lti-ri.imsglobal.org/platforms/774/authorizations/new");
            iss1b.setDeploymentId("0002");
            iss1b.setToolKid("hf8Sisblt0zj0KhjY8oAIH0ylU2PuYwnegc8Y9vJq9g");
            iss1b.setPlatformKid("hf8Sisblt0zj0KhjY8oAIH0ylU2PuYwnegc8Y9vJq9g");
            String tool1bPrivateString = "-----BEGIN RSA PRIVATE KEY-----" +
                    "MIIEpAIBAAKCAQEA1emJEYJebrnPAvrAf6FDCQAOldKF3W+LY8i91L3NvUPgrkKs" +
                    "PjjRO+g0B+sRqKsoWVaN8wZ2j0y+e2YX5+ig1k2bMmNHMgRGISf1rvgMEJA1k9Ri" +
                    "GxWuMeWrP9Aa/nYEs7Wau5dCB0SelGCPHEjrHmHmIzfZGsJG/i1AZ7EKOER90cxQ" +
                    "G3pG8tnQqWNordtxJ7Cqr2/jSAFb5zW++AV9D6xjlSTuk1V3uJbtEH4q2Zid8fA8" +
                    "aAwaNPvL7QbW5IhrZw/chGxD/z3wHb1VQFiyycVjI6LTTmzI4IB9Dkt6QS3jzxft" +
                    "+AkTsJ4250xbCYr2lWsbd1n1+E3uzjipOS5EGQIDAQABAoIBAE0oUneNVbCMtv21" +
                    "IrAmo75gVeJ2sDBiJp4Ub1yIJejJzgYtKGG9LsN0Cyh8Ar+bFQ+8Z7EsOKGRpfdu" +
                    "qNrOjw0dqwguxSRmZEFbX4QAFqH20kyDQ+vPEykOVYnL76CvQxzrOWfGtFtYxfZx" +
                    "Kc2jA6PO3ir/3wCG6QKPofaE5lHKwcpPfacV+1LL7I+NXa1uGzg5aZ2wKH7j2A3R" +
                    "6BJvflwbHFJtDFbicFBtb11ZtHKX6qhMo6Keum/vDglGJam07nPEcCtT+WtPPhF9" +
                    "yKVeMCbXlI9QxcdMyCpQSGt2Sm/8YO5zabnnyZVlws9SBqq5Zd3M520UHC8BQ4aY" +
                    "kjFZ5P0CgYEA8erlhZyYNZysAv4HwynPSAdcY5W1nImQMvGm3NN2cBPs2fevl560" +
                    "DeQn1G+60LGsYRpCl8aVok+mF9rc5yXsl823vB2NQu6/DoT4f753bt6sF6WlqkV5" +
                    "9Cun3DToXxDU9IfcSEge21b4FxMYmwleSmtVfiB8p6n3qDrOoYB+mzMCgYEA4l1M" +
                    "TsXBdJpQ2tU8mIA6HUDlzp7bnlrDWJzoZA+0rMGZZCZZ0D9V0icF3ht7oVMYqGY8" +
                    "ROUDfYJbq7qHbcUOgog8Furs25+iNAM5nlcEsaowdz6kqPxFm2U2Koc0aQSWToPo" +
                    "RLzL6ejggRacXRua5nUwnv8Vx3oKT7eua88fw4MCgYEA1BWZmNRbQI0U5B6u0XNj" +
                    "DIOfhJKoQA79wxvFrM0ahVGCkOirISJ6Ob9vB7fYMMPDGvH5tbPcVQq80ycGCQNf" +
                    "cwpf7OR/hlFmYCVE8kEZ1bITbzvCjA8SxnRLWitsGIPaHnLJNPk9TA/nudr89FZ3" +
                    "Ooj0z3lNr3O78dl0c3QCCq8CgYEA23TaUxBUMqidNNtAqRS/wra1VXEbuFWER7ev" +
                    "cbrsTgRPoxGvRz7wBBMDFEcOv+Og5zpeuehRTu//0ei//YLrQ0+y+gD+axpDlit+" +
                    "Q+1XRquZ2zGnT5FsJnCkZ+y2ug3RbwNhPqrPAtJcPapfI8FslnsNDUh+o+rEbm7E" +
                    "sg6XW+MCgYBDEQpTlxjBg6ulpfMkZ69SCJ00MFrNwVHbkDNMjLmjfYsstqYgJMUT" +
                    "A/TVaslSL07p12NgjKcAQ4I1uoVM9CREiXLXN+RvXr8OfIvgsrpcwZ45/Vni/vLo" +
                    "xDXNkg2Lf9/J5A4d/ibZCuZYoqapdjVIJ5L13Wo540Md7zLDKHyB9w==" +
                    "-----END RSA PRIVATE KEY-----";
            String tool1bPublicString ="-----BEGIN PUBLIC KEY-----" +
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1emJEYJebrnPAvrAf6FD" +
                    "CQAOldKF3W+LY8i91L3NvUPgrkKsPjjRO+g0B+sRqKsoWVaN8wZ2j0y+e2YX5+ig" +
                    "1k2bMmNHMgRGISf1rvgMEJA1k9RiGxWuMeWrP9Aa/nYEs7Wau5dCB0SelGCPHEjr" +
                    "HmHmIzfZGsJG/i1AZ7EKOER90cxQG3pG8tnQqWNordtxJ7Cqr2/jSAFb5zW++AV9" +
                    "D6xjlSTuk1V3uJbtEH4q2Zid8fA8aAwaNPvL7QbW5IhrZw/chGxD/z3wHb1VQFiy" +
                    "ycVjI6LTTmzI4IB9Dkt6QS3jzxft+AkTsJ4250xbCYr2lWsbd1n1+E3uzjipOS5E" +
                    "GQIDAQAB" +
                    "-----END PUBLIC KEY-----";
            rsaKeyRepository.saveAndFlush(new RSAKeyEntity("354a0d626b1523a193e5",true, tool1bPublicString,tool1bPrivateString));
            rsaKeyRepository.saveAndFlush(new RSAKeyEntity("354a0d626b1523a193e5",false, iss1bPublicKey,null));

            platformDeploymentRepository.saveAndFlush(iss1b);


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
            iss5.setClientId("6482545f-91ec-4e33-8d9c-96f069995312");
            iss5.setIss("https://blackboard.com");
            String iss5PublicKey = "";
            iss5.setJwksEndpoint("https://developer.blackboard.com/api/v1/management/applications/6482545f-91ec-4e33-8d9c-96f069995312/jwks.json");
            iss5.setoAuth2TokenUrl("https://developer.blackboard.com/api/v1/gateway/oauth2/jwttoken");
            iss5.setOidcEndpoint("https://developer.blackboard.com/api/v1/gateway/oauth2/jwttoken");
            //iss5.setDeploymentId("b81107cf-a265-40f9-bf2d-91ee9245208c");
            iss5.setToolKid("6482545f-91ec-4e33-8d9c-96f069995312");
            iss5.setPlatformKid("6482545f-91ec-4e33-8d9c-96f069995312");
            String tool5PrivateString = "-----BEGIN PRIVATE KEY-----" +
                    "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCh3WHLNTXQ2U2h" +
                    "c6EbRosOyXspKMFdOpKXCI5Doe+bm4kCcVeSFsGaworyMhvl9DmCVs9UeW6Ys32r" +
                    "Y9znoQDV0g3PE1lfViOA825JaPgt3GxitugsGBHJX3tBrqQMyxBeBPpEVCoRUUSX" +
                    "67S0tpwSmDX7ohPoKD5gdYkSjhU4W+YRqYTBsc4kA/DRxFxMPH94SSMWIIeFXhb4" +
                    "ghOCzFsXKzcjgSnie70iBjHOOa94kQxr7icpJjdHMUadWXU/3qqCtlO8J6qPXlnh" +
                    "vIlsYbNxc+cY0Z6yUbBcn6KIo+lctvcQm4lEoyV9Fv6n5V8eboKliuAE19RqfyNQ" +
                    "7RJ0C5/DAgMBAAECggEAY2fJ/zoWasSJYHXaox2XmOUztftJhS/LhuXCONbih/Xl" +
                    "FpL07Mr860Y+fq83Yumxx9H4UjChMzZIH1GdAMNn9+iggmOnp10HQNI/EOZeRAvy" +
                    "pE0gGLRPeBQjZhsStKigzJzR9dSaYTg/n+0pdTIQd3ry7C6FezX72NFV9Qc9EPaj" +
                    "zzHahSeztg8vkZiq87pa+CHCsJcgp+TFD5ZuKJj16ETE5HCvowl5Ap+B8ReHOqZs" +
                    "q5U6UTHH90kU3GcM5N74+NAwsw+CTfNShkoh3IO1pSbI1J0wf9QqKVw2YKwpGQin" +
                    "MqgcMeOT7FUao0aMHjYz9Sovgo2gHrtU8mMqtYVuAQKBgQDiQYvEQp+vUS7DxRE/" +
                    "fuGBl+UYTggHiLHKylZn/d/sMi8qeS4NFBXwksHzIVQ75ikK4+etE6iR2jceA0XH" +
                    "v2SXP1S4v7z9BQCuFv9kLFYfm8n/ylzljRXeiCn5wh5b8/FYVVXaSZGPZEXEpZJi" +
                    "iACzksV8DGI4ma6ublSsAWibYwKBgQC3JM2MCm3pGo+705sQ6wnJzkv0rstm6Dvc" +
                    "ty4tfAY9u3IenrAAcJxFSFSMunkdxEm76JeP31/fEjFKgcey6+ucwSkQJwF3KT4R" +
                    "mgNDbuzhYIzcf1FTcj4/26UI0HqZ18pz3Z+nlvr05Ta/bQV+UBd+tIt0Rjqq2Yti" +
                    "BYBZq+GIIQKBgQDRBZ7avHYLoCNk5u1NTKmGcM5MCuKuGyGtaAo1Xjv9WEtsoLDj" +
                    "kjfGnb4iIBtSjwpBrlS895lWzVL4HivcIjwK4o4hc7ljwrhjHInqgG88Gk+eSNRT" +
                    "mWinwgGNZEFpz86aPZPn6ulXoFo4FoJLFrwXKbPaKxKemZPgP7tngJGkYwKBgQCr" +
                    "uEVGPm0p+O62NUWbClZmMxlC9jXfanbej58rpLORZFUvYqkx8GSGDbjBLwLrJyXf" +
                    "m8moa9BPr5Yp/x/Ioq6Ljw26iGg2W6RICrlum+5dsBLDmFkfvemiJIdMiXW7E/tO" +
                    "wom1MLpjrxuzRzy0X9J3yj9LqaHOmiW3peLfsWrJIQKBgQCLt5BX5XA1Wy2KtFA4" +
                    "zdHyVSrWbKBYwm1Dsxx2hRqPVoVWGW5xwjtzRXEpWlKnnJF8el3lMppfNG5+65J/" +
                    "pxfn/6nk1ofEFM/YEh+xLXeHuyH7d/1NYRkae03GZtat8YAfXbJ9JQIhAHdsOBY9" +
                    "XvlT462gBx2/M2sXXNzEJY2oxg==" +
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
            LtiUserEntity user = ltiUserRepository.saveAndFlush(new LtiUserEntity("azeckoski", null, null));
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
