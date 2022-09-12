package net.unicon.lti.controller.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping(value = "/valkyrie")
@ConditionalOnExpression("${lti13.enableMockValkyrie}")
public class MockValkyrieController {
    @Value("${harmony.courses.api}")
    private String localUrl;

    @RequestMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> catalog() {
        String catalog = "{\n" +
                "  \"records\": [\n" +
                "    {\n" +
                "      \"root_outcome_guid\": \"357365c8-fd91-415b-8139-aa19779dabdd\",\n" +
                "      \"book_title\": \"Lifespan Development - F19\",\n" +
                "      \"release_date\": null,\n" +
                "      \"cover_img_url\": \"https://oer-catalog-images.s3-us-west-2.amazonaws.com/LumenLearning/Lifespan-Development_Cover.jpg\",\n" +
                "      \"category\": null,\n" +
                "      \"description\": \"This course covers the growth and development through the lifespan—including physical, cognitive and socioemotional changes through each stage of life.\",\n" +
                "      \"table_of_contents\": [\n" +
                "        {\n" +
                "          \"name\": \"Succeeding With Waymaker\",\n" +
                "          \"module_id\": \"4a065e1c-6071-4be0-a0bb-a53d187961b4\",\n" +
                "          \"sub_topics\": [\n" +
                "            \"Assignment: Research Consent and Communication Preferences\",\n" +
                "            \"Practice, Practice, Practice\",\n" +
                "            \"Get To The Starting Line\",\n" +
                "            \"Diving In & Finishing Strong\",\n" +
                "            \"Multiple Attempts & Multiple Answers\",\n" +
                "            \"Can My Quiz Attempt Get Lost?\",\n" +
                "            \"What Are the Technical Requirements?\"\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"Introducing Lifespan Development\",\n" +
                "          \"module_id\": \"e79399c0-a14b-4d25-aaae-701690d7d341\",\n" +
                "          \"sub_topics\": [\n" +
                "            \"Why It Matters\",\n" +
                "            \"Discussion: Life Stages\",\n" +
                "            \"Assignment: Lifespan Development in the News\",\n" +
                "            \"Show What You Know\",\n" +
                "            \"Introduction to Human Development\",\n" +
                "            \"Introduction to the Lifespan Perspective\",\n" +
                "            \"Introduction to Research in Lifespan Development\",\n" +
                "            \"Putting It Together\",\n" +
                "            \"Quiz Prep\"\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"Theories in Lifespan Development\",\n" +
                "          \"module_id\": \"46a574d0-83c4-4fc1-911b-18c97df646b3\",\n" +
                "          \"sub_topics\": [\n" +
                "            \"Why It Matters\",\n" +
                "            \"Discussion: Developmental Theories\",\n" +
                "            \"Assignment: Applying Developmental Theories\",\n" +
                "            \"Assignment: Bioecological Model Journal\",\n" +
                "            \"Show What You Know\",\n" +
                "            \"Introduction to Psychodynamic Theories\",\n" +
                "            \"Introduction to Behavioral and Cognitive Theories\",\n" +
                "            \"Introduction to the Humanistic, Contextual, and Evolutionary Perspectives of Development\",\n" +
                "            \"Putting It Together\",\n" +
                "            \"Quiz Prep\"\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"Prenatal Development\",\n" +
                "          \"module_id\": \"6af7a6be-1ebb-44a6-9fa7-ff1949cf772b\",\n" +
                "          \"sub_topics\": [\n" +
                "            \"Why It Matters\",\n" +
                "            \"Discussion: Prenatal Development\",\n" +
                "            \"Assignment: Pregnancy and Birth\",\n" +
                "            \"Show What You Know\",\n" +
                "            \"Assignment: Birth Plan\",\n" +
                "            \"Assignment: Birth Journal\",\n" +
                "            \"Introduction to Biological Foundations of Human Development\",\n" +
                "            \"Introduction to Prenatal Development\",\n" +
                "            \"Introduction to Birth and Delivery\",\n" +
                "            \"Putting It Together\",\n" +
                "            \"Quiz Prep\"\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"Infancy\",\n" +
                "          \"module_id\": \"9d70c772-5f1f-41bc-8014-f4029f1975c0\",\n" +
                "          \"sub_topics\": [\n" +
                "            \"Why It Matters\",\n" +
                "            \"Discussion: Infancy\",\n" +
                "            \"Assignment: Hot Topic Infographic\",\n" +
                "            \"Show What You Know\",\n" +
                "            \"Introduction to Physical Growth and Development in Newborns and Toddlers\",\n" +
                "            \"Introduction to Cognitive Development in Infants and Toddlers\",\n" +
                "            \"Introduction to Emotional and Social Development During Infancy\",\n" +
                "            \"Putting It Together\",\n" +
                "            \"Quiz Prep\"\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"Early Childhood\",\n" +
                "          \"module_id\": \"67238ceb-d24a-43a8-aef3-3cb5dd8b8a48\",\n" +
                "          \"sub_topics\": [\n" +
                "            \"Why It Matters\",\n" +
                "            \"Discussion: Parenting Styles\",\n" +
                "            \"Assignment: Children's Toys\",\n" +
                "            \"Show What You Know\",\n" +
                "            \"Assignment: Children's Media\",\n" +
                "            \"Assignment: Preschool Journal\",\n" +
                "            \"Introduction to Physical Development in Early Childhood\",\n" +
                "            \"Introduction to Cognitive Development in Early Childhood\",\n" +
                "            \"Introduction to Emotional and Social Development in Early Childhood\",\n" +
                "            \"Putting It Together\",\n" +
                "            \"Quiz Prep\"\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"Middle Childhood\",\n" +
                "          \"module_id\": \"ce37c545-7c88-4c99-b37b-e97cd96ae666\",\n" +
                "          \"sub_topics\": [\n" +
                "            \"Why It Matters\",\n" +
                "            \"Discussion: Middle Childhood\",\n" +
                "            \"Assignment: Anti-Bullying Ad\",\n" +
                "            \"Show What You Know\",\n" +
                "            \"Assignment: Moral Reasoning Interview\",\n" +
                "            \"Introduction to Physical Development in Middle Childhood\",\n" +
                "            \"Introduction to Cognitive Development in Middle Childhood\",\n" +
                "            \"Introduction to Educational Issues during Middle Childhood\",\n" +
                "            \"Introduction to Emotional and Social Development in Middle Childhood\",\n" +
                "            \"Putting It Together\",\n" +
                "            \"Quiz Prep\"\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"Adolescence\",\n" +
                "          \"module_id\": \"684254bd-7aab-4e22-be2d-563f7f41a20c\",\n" +
                "          \"sub_topics\": [\n" +
                "            \"Why It Matters\",\n" +
                "            \"Discussion: Adolescence\",\n" +
                "            \"Assignment: Build an Interactive\",\n" +
                "            \"Discussion: Adolescence Interview Assignment\",\n" +
                "            \"Show What You Know\",\n" +
                "            \"Introduction to Physical Growth and Development in Adolescence\",\n" +
                "            \"Introduction to Cognitive Development in Adolescence\",\n" +
                "            \"Introduction to Emotional and Social Development in Adolescence\",\n" +
                "            \"Putting It Together\",\n" +
                "            \"Quiz Prep\"\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"Early Adulthood\",\n" +
                "          \"module_id\": \"d35f631d-7563-486b-a8f7-d601e55fd4ec\",\n" +
                "          \"sub_topics\": [\n" +
                "            \"Why It Matters\",\n" +
                "            \"Discussion: Early Adulthood\",\n" +
                "            \"Assignment: Emerging Adulthood in the Media\",\n" +
                "            \"Discussion: Dating and Marriage Interview Assignment\",\n" +
                "            \"Show What You Know\",\n" +
                "            \"Assignment: My Development Journal\",\n" +
                "            \"Introduction to Physical Development in Early Adulthood\",\n" +
                "            \"Introduction to Cognitive Development in Early Adulthood\",\n" +
                "            \"Introduction to Theories of Adult Psychosocial Development\",\n" +
                "            \"Introduction to Relationships in Early Adulthood\",\n" +
                "            \"Putting It Together\",\n" +
                "            \"Quiz Prep\"\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"Middle Adulthood\",\n" +
                "          \"module_id\": \"8e2ce6c6-19cf-4a97-97c2-1046a4e58e86\",\n" +
                "          \"sub_topics\": [\n" +
                "            \"Why It Matters\",\n" +
                "            \"Discussion: Middle Adulthood\",\n" +
                "            \"Assignment: Applications of Erikson's Stages\",\n" +
                "            \"Discussion: Adulthood Interview Assignment\",\n" +
                "            \"Show What You Know\",\n" +
                "            \"Introduction to Physical Development in Middle Adulthood\",\n" +
                "            \"Introduction to Cognitive Development in Middle Adulthood\",\n" +
                "            \"Introduction to Emotional and Social Development in Middle Adulthood\",\n" +
                "            \"Introduction to Relationships in Middle Adulthood\",\n" +
                "            \"Putting It Together\",\n" +
                "            \"Quiz Prep\"\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"Late Adulthood\",\n" +
                "          \"module_id\": \"7fb5a03e-ca5a-489e-ab68-d931b81ecddd\",\n" +
                "          \"sub_topics\": [\n" +
                "            \"Why It Matters\",\n" +
                "            \"Discussion: Late Adulthood\",\n" +
                "            \"Assignment: Defining Happiness\",\n" +
                "            \"Discussion: Late Adulthood Interview Assignment\",\n" +
                "            \"Show What You Know\",\n" +
                "            \"Assignment: Aging Journal\",\n" +
                "            \"Introduction to Physical Development in Late Adulthood\",\n" +
                "            \"Introduction to Cognitive Development in Late Adulthood\",\n" +
                "            \"Introduction to Psychosocial Development in Late Adulthood\",\n" +
                "            \"Putting It Together\",\n" +
                "            \"Quiz Prep\"\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"Death and Dying\",\n" +
                "          \"module_id\": \"86ba48fb-927a-4500-b75f-49db29e453a1\",\n" +
                "          \"sub_topics\": [\n" +
                "            \"Why It Matters\",\n" +
                "            \"Discussion: Death and Dying\",\n" +
                "            \"Assignment: Bucket List\",\n" +
                "            \"Show What You Know\",\n" +
                "            \"Introduction to Understanding Death\",\n" +
                "            \"Introduction to Emotions Related to Death\",\n" +
                "            \"Introduction to Facing Death\",\n" +
                "            \"Putting It Together\",\n" +
                "            \"Quiz Prep\"\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"root_outcome_guid\": \"f9515fc8-d4f8-433e-aa20-742748d08cdf\",\n" +
                "      \"book_title\": \"Deep Linking Interim Test Course11\",\n" +
                "      \"release_date\": null,\n" +
                "      \"cover_img_url\": null,\n" +
                "      \"category\": null,\n" +
                "      \"description\": null,\n" +
                "      \"table_of_contents\": []\n" +
                "    },\n" +
                "    {\n" +
                "      \"root_outcome_guid\": \"7c2fbcd3-dc69-49f1-a745-235abc2c008c\",\n" +
                "      \"book_title\": \"Deep Linking Interim Test Course12\",\n" +
                "      \"release_date\": null,\n" +
                "      \"cover_img_url\": null,\n" +
                "      \"category\": null,\n" +
                "      \"description\": null,\n" +
                "      \"table_of_contents\": []\n" +
                "    },\n" +
                "    {\n" +
                "      \"root_outcome_guid\": \"c7756f3c-2c51-403e-90c4-fdcea7165152\",\n" +
                "      \"book_title\": \"Deep Linking Interim Test Course13\",\n" +
                "      \"release_date\": null,\n" +
                "      \"cover_img_url\": null,\n" +
                "      \"category\": null,\n" +
                "      \"description\": null,\n" +
                "      \"table_of_contents\": []\n" +
                "    },\n" +
                "    {\n" +
                "      \"root_outcome_guid\": \"82299158-cc57-459e-be93-7061fe246c2d\",\n" +
                "      \"book_title\": \"Deep Linking Interim Test Course14\",\n" +
                "      \"release_date\": null,\n" +
                "      \"cover_img_url\": null,\n" +
                "      \"category\": null,\n" +
                "      \"description\": null,\n" +
                "      \"table_of_contents\": []\n" +
                "    },\n" +
                "    {\n" +
                "      \"root_outcome_guid\": \"root\",\n" +
                "      \"book_title\": \"Mini Course for Exemplar Testing\",\n" +
                "      \"release_date\": null,\n" +
                "      \"cover_img_url\": null,\n" +
                "      \"category\": null,\n" +
                "      \"description\": null,\n" +
                "      \"table_of_contents\": []\n" +
                "    }\n" +
                "  ],\n" +
                "  \"metadata\": {\n" +
                "    \"page\": \"1\",\n" +
                "    \"per_page\": \"10\",\n" +
                "    \"page_count\": 1,\n" +
                "    \"total_count\": 6,\n" +
                "    \"links\": {\n" +
                "      \"first\": \"/service_api/course_catalog?page=1\",\n" +
                "      \"last\": \"/service_api/course_catalog?page=1\",\n" +
                "      \"prev\": \"/service_api/course_catalog?page=1\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        return new ResponseEntity<>(catalog, HttpStatus.OK);
    }

    @RequestMapping(value="/lineitems", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> lineitems() {
        String lineitems = "{\n" +
                "    \"root_outcome_guid\": \"357365c8-fd91-415b-8139-aa19779dabdd\"\n" +
                "  }";
        return new ResponseEntity<>(lineitems, HttpStatus.OK);
    }

    @RequestMapping(value="/lti_deep_links", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> ltiDeepLinks(@RequestParam(required = false, value = "course_paired") boolean coursePaired) {
        String deepLinks = "[\n" +
                "    {\n" +
                "      \"title\": \"Assignment: Research Consent and Communication Preferences\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 5,\n" +
                "      \"label\": \"Assignment: Research Consent and Communication Preferences\",\n" +
                "      \"resourceId\": \"34908919-b8da-4538-880e-bdd90c0ee4ae\",\n" +
                "      \"tag\": \"assignment\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Study Plan: Introducing Lifespan Development\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 10,\n" +
                "      \"label\": \"Study Plan: Introducing Lifespan Development\",\n" +
                "      \"resourceId\": \"19050609-42fd-4824-b7a3-9be5726df492\",\n" +
                "      \"tag\": \"study plan\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Quiz: Introducing Lifespan Development\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 25,\n" +
                "      \"label\": \"Quiz: Introducing Lifespan Development\",\n" +
                "      \"resourceId\": \"4a00c18e-5ce0-4583-9c8f-16fa28f0bde9\",\n" +
                "      \"tag\": \"quiz\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Study Plan: Theories in Lifespan Development\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 10,\n" +
                "      \"label\": \"Study Plan: Theories in Lifespan Development\",\n" +
                "      \"resourceId\": \"a75b7ee7-90b5-47bd-be5d-2a0c00098eab\",\n" +
                "      \"tag\": \"study plan\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Quiz: Theories in Lifespan Development\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 25,\n" +
                "      \"label\": \"Quiz: Theories in Lifespan Development\",\n" +
                "      \"resourceId\": \"2bed541d-a559-488a-99df-56289456761d\",\n" +
                "      \"tag\": \"quiz\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Study Plan: Prenatal Development\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 10,\n" +
                "      \"label\": \"Study Plan: Prenatal Development\",\n" +
                "      \"resourceId\": \"d7f5b322-581a-4026-9453-923559c94599\",\n" +
                "      \"tag\": \"study plan\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Quiz: Prenatal Development\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 25,\n" +
                "      \"label\": \"Quiz: Prenatal Development\",\n" +
                "      \"resourceId\": \"89427d81-211e-4369-924c-d2a1beed57fb\",\n" +
                "      \"tag\": \"quiz\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Study Plan: Infancy\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 10,\n" +
                "      \"label\": \"Study Plan: Infancy\",\n" +
                "      \"resourceId\": \"73883263-3a12-47e8-8658-dde2281286c1\",\n" +
                "      \"tag\": \"study plan\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Quiz: Infancy\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 25,\n" +
                "      \"label\": \"Quiz: Infancy\",\n" +
                "      \"resourceId\": \"495bf61f-4324-4eb3-b39e-aff3068aba8a\",\n" +
                "      \"tag\": \"quiz\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Study Plan: Early Childhood\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 10,\n" +
                "      \"label\": \"Study Plan: Early Childhood\",\n" +
                "      \"resourceId\": \"1819af6f-cd02-4eeb-9641-67dd581b0dc3\",\n" +
                "      \"tag\": \"study plan\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Quiz: Early Childhood\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 25,\n" +
                "      \"label\": \"Quiz: Early Childhood\",\n" +
                "      \"resourceId\": \"fb2860a2-2b5e-4074-ae12-bbc3e4f13f97\",\n" +
                "      \"tag\": \"quiz\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Study Plan: Middle Childhood\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 10,\n" +
                "      \"label\": \"Study Plan: Middle Childhood\",\n" +
                "      \"resourceId\": \"7ad3d161-9706-4a5c-bf7b-9acc86b7a4d8\",\n" +
                "      \"tag\": \"study plan\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Quiz: Middle Childhood\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 25,\n" +
                "      \"label\": \"Quiz: Middle Childhood\",\n" +
                "      \"resourceId\": \"68e9c2fc-ab9a-475f-945f-6731e8ce432f\",\n" +
                "      \"tag\": \"quiz\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Study Plan: Adolescence\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 10,\n" +
                "      \"label\": \"Study Plan: Adolescence\",\n" +
                "      \"resourceId\": \"7c5856a3-42eb-4144-85ab-7a54ef697545\",\n" +
                "      \"tag\": \"study plan\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Quiz: Adolescence\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 25,\n" +
                "      \"label\": \"Quiz: Adolescence\",\n" +
                "      \"resourceId\": \"87f10a37-2659-423b-96b3-4436a6dd2132\",\n" +
                "      \"tag\": \"quiz\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Study Plan: Early Adulthood\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 10,\n" +
                "      \"label\": \"Study Plan: Early Adulthood\",\n" +
                "      \"resourceId\": \"f3471a3e-d97d-46f9-b066-0fca1ed9e0c8\",\n" +
                "      \"tag\": \"study plan\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Quiz: Early Adulthood\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 25,\n" +
                "      \"label\": \"Quiz: Early Adulthood\",\n" +
                "      \"resourceId\": \"21b4dd51-a8e8-44a1-aac2-ae8d46a7645e\",\n" +
                "      \"tag\": \"quiz\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Study Plan: Middle Adulthood\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 10,\n" +
                "      \"label\": \"Study Plan: Middle Adulthood\",\n" +
                "      \"resourceId\": \"cbeda787-9c42-4759-a9e0-750da95fbf68\",\n" +
                "      \"tag\": \"study plan\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Quiz: Middle Adulthood\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 25,\n" +
                "      \"label\": \"Quiz: Middle Adulthood\",\n" +
                "      \"resourceId\": \"6ef6e199-0355-4516-9e9e-98d860302376\",\n" +
                "      \"tag\": \"quiz\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Study Plan: Late Adulthood\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 10,\n" +
                "      \"label\": \"Study Plan: Late Adulthood\",\n" +
                "      \"resourceId\": \"0d987f09-8a8d-425a-a671-ebc7bf4db3ac\",\n" +
                "      \"tag\": \"study plan\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Quiz: Late Adulthood\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 25,\n" +
                "      \"label\": \"Quiz: Late Adulthood\",\n" +
                "      \"resourceId\": \"0d87775f-b174-44b6-8a74-b4663e7b53b4\",\n" +
                "      \"tag\": \"quiz\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Study Plan: Death and Dying\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 10,\n" +
                "      \"label\": \"Study Plan: Death and Dying\",\n" +
                "      \"resourceId\": \"a0bb3807-ec52-4805-8c59-71c156ec44a0\",\n" +
                "      \"tag\": \"study plan\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Quiz: Death and Dying\",\n" +
                "      \"url\": \"" + localUrl + "/study_plan\",\n" +
                "      \"scoreMaximum\": 25,\n" +
                "      \"label\": \"Quiz: Death and Dying\",\n" +
                "      \"resourceId\": \"f34f3199-d56a-40f6-a4ac-3af3c16a40c2\",\n" +
                "      \"tag\": \"quiz\"\n" +
                "    }\n" +
                "  ]";
        return new ResponseEntity<>(deepLinks, HttpStatus.OK);
    }
    
    @RequestMapping(value = "study_plan")
    public ResponseEntity<String> studyPlan() {
        return new ResponseEntity<>("Hello, Study Plan!", HttpStatus.OK);
    }
}
