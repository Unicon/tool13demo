// This file is for development purposes when developing with React locally.
const TOC_ARRAY = [
  {
      name: "Course Contents",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-0",
      sub_topics: [
          "About This Course",
          "Course Contents at a Glance",
          "Learning Outcomes"
      ]
  },
  {
      name: "Faculty Resources",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-1",
      sub_topics: [
          "Waymaker Faculty Tools",
          "Faculty Resources Overview",
          "Pacing",
          "Offline Content Access",
          "PowerPoints",
          "Assignments",
          "Discussions with Solutions",
          "Interactives",
          "Question Banks",
          "I Need Help"
      ]
  },
  {
      name: "Succeeding With Waymaker",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-2",
      sub_topics: [
          "Assignment: Research Consent and Communication Preferences",
          "Practice, Practice, Practice",
          "Get To The Starting Line",
          "Diving In & Finishing Strong",
          "Multiple Attempts & Multiple Answers",
          "Can My Quiz Attempt Get Lost?",
          "What Are the Technical Requirements?"
      ]
  },
  {
      name: "The Role of Human Resources",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-3",
      sub_topics: [
          "Why It Matters",
          "Discussion: The Role of Human Resources",
          "Show What You Know",
          "Introduction to Defining Human Resources Management",
          "Introduction to Functions of Human Resources Management",
          "Putting It Together",
          "Quiz Prep"
      ]
  },
  {
      name: "Human Resource Strategy & Planning",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-4",
      sub_topics: [
          "Why It Matters",
          "Discussion: Human Resource Strategy and Planning",
          "Assignment: Becoming a Changemaker",
          "Show What You Know",
          "Introduction to Foundations of Human Resources Management",
          "Introduction to Strategic Management",
          "Introduction to Strategic Planning",
          "Putting It Together",
          "Quiz Prep"
      ]
  },
  {
      name: "People Analytics and Human Capital Trends",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-5",
      sub_topics: [
          "Why It Matters",
          "Discussion: People Analytics and Human Capital Trends",
          "Show What You Know",
          "Introduction to People Analytics",
          "Introduction to Strategy and People Analytics",
          "Introduction to Human Capital Trends",
          "Putting It Together",
          "Quiz Prep"
      ]
  },
  {
      name: "Diversity in the Workplace",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-6",
      sub_topics: [
          "Why It Matters",
          "Discussion: Diversity in the Workplace",
          "Assignment: Develop a Diversity Allies Program",
          "Show What You Know",
          "Introduction to Legislation",
          "Introduction to Equal Employment Opportunity",
          "Introduction to Working with a Diverse Workforce",
          "Introduction to Promoting a Diverse Workforce",
          "Introduction to Current Diversity-Related Trends",
          "Putting It Together",
          "Quiz Prep"
      ]
  },
  {
      name: "Workforce Planning",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-7",
      sub_topics: [
          "Why It Matters",
          "Discussion: Workforce Planning",
          "Assignment: Job Description Research and Development",
          "Show What You Know",
          "Introduction to the Workforce Planning Process",
          "Introduction to the Job Analysis Process",
          "Introduction to Job Descriptions",
          "Introduction to Job Design",
          "Putting It Together",
          "Quiz Prep"
      ]
  },
  {
      name: "Recruitment and Selection",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-8",
      sub_topics: [
          "Assignment: Employer Branding Overview",
          "Why It Matters",
          "Discussion: Recruitment and Selection",
          "Show What You Know",
          "Introduction to the Recruitment Process",
          "Introduction to Recruitment Sources",
          "Introduction to Avoiding Discrimination",
          "Introduction to the Selection Process",
          "Putting It Together",
          "Quiz Prep"
      ]
  },
  {
      name: "Onboarding, Training, and Developing Employees",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-9",
      sub_topics: [
          "Why It Matters",
          "Discussion: Onboarding, Training, and Developing Employees",
          "Assignment: Effective Onboarding Presentation",
          "Show What You Know",
          "Introduction to Onboarding Employees",
          "Introduction to Training Employees",
          "Introduction to Developing Employees",
          "Putting It Together",
          "Quiz Prep"
      ]
  },
  {
      name: "Compensation and Benefits",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-10",
      sub_topics: [
          "Why It Matters",
          "Discussion: Compensation and Benefits",
          "Show What You Know",
          "Introduction to The Law and Compensation",
          "Introduction to Types of Compensation",
          "Introduction to Benefits and Benefit Trends",
          "Putting It Together",
          "Quiz Prep"
      ]
  },
  {
      name: "Performance Management and Appraisal",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-11",
      sub_topics: [
          "Why It Matters",
          "Discussion: Performance Management and Appraisal",
          "Show What You Know",
          "Introduction to Performance Management",
          "Introduction to the Appraisal Process",
          "Introduction to Appraisal Effectiveness",
          "Putting It Together",
          "Quiz Prep"
      ]
  },
  {
      name: "Building Positive Employee Relations",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-12",
      sub_topics: [
          "Why It Matters",
          "Discussion: Building Positive Employee Relations",
          "Assignment: Performance Appraisal Training",
          "Show What You Know",
          "Introduction to Employee Engagement",
          "Introduction to Conflict at Work",
          "Putting It Together",
          "Quiz Prep"
      ]
  },
  {
     name: "Employee Termination",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-13",
     sub_topics: [
          "Discussion: Employee Termination",
          "Why It Matters",
          "Show What You Know",
          "Introduction to Downsizing",
          "Introduction to Effects of Termination",
          "Putting It Together",
          "Quiz Prep"
      ]
  },
  {
      name: "Employee Rights and Responsibilities",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-14",
      sub_topics: [
          "Why It Matters",
          "Discussion: Employee Rights and Responsibilities",
          "Show What You Know",
          "Introduction to Laws and Employee Rights",
          "Introduction to Employee-Employer Contracts",
          "Introduction to Disciplinary Factors and Guidelines",
          "Putting It Together",
          "Quiz Prep"
      ]
  },
  {
      name: "Union–Management Relations",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-15",
      sub_topics: [
          "Why It Matters",
          "Discussion: Union–Management Relations",
          "Assignment: The Future of Unions",
          "Show What You Know",
          "Introduction to Workers and Unions",
          "Introduction to Laws and Unions",
          "Introduction to the Role of Unions",
          "Putting It Together",
          "Quiz Prep"
      ]
  },
  {
      name: "Safety, Health, and Risk Management",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-16",
      sub_topics: [
          "Why It Matters",
          "Discussion: Safety, Health, and Risk Management",
          "Show What You Know",
          "Introduction to Workplace Safety and Health",
          "Introduction to Inspection and Enforcement",
          "Introduction to Improving Workplace Safety",
          "Introduction to Risk Management",
          "Putting It Together",
          "Quiz Prep"
      ]
  },
  {
      name: "Corporate Social Responsibility",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-17",
      sub_topics: [
          "Assignment: HR’s Sustainability Role",
          "Why It Matters",
          "Discussion: Corporate Social Responsibility",
          "Show What You Know",
          "Introduction to Ethics",
          "Introduction to Sustainability",
          "Introduction to Corporate Social Responsibility",
          "Putting It Together",
          "Quiz Prep"
      ]
  },
  {
      name: "Global Human Resources",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-18",
      sub_topics: [
          "Why It Matters",
          "Discussion: Global Human Resources",
          "Assignment: Mastering Multiculturalism",
          "Show What You Know",
          "Introduction to the Global Environment",
          "Introduction to Global Employee Engagement",
          "Putting It Together",
          "Quiz Prep"
      ]
  },
  {
      name: "Human Resources in Small and Entrepreneurial Businesses",
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-19",
      sub_topics: [
          "Why It Matters",
          "Discussion: Human Resources in Small and Entrepreneurial Businesses",
          "Assignment: Skills for the Future",
          "Show What You Know",
          "Assignment: HR with Limited Resources",
          "Introduction to Determining HR Needs",
          "Introduction to Employment Laws and Small Business",
          "Introduction to Essential HR Resources",
          "Introduction to Hiring for the Start-up Environment",
          "Putting It Together",
          "Quiz Prep"
      ]
  },
  {
      name: null,
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-20",
      sub_topics: null
  },
  {
      name: null,
      module_id: "4a065e1c-6071-4be0-a0bb-a53d187961b4-21",
      sub_topics: []
  }
];

export const DUMMY_DATA = {
    records: [
      {
        root_outcome_guid: "0a990cb3-8cde-4e9b-a99a-b4caaea82eee1",
        book_title: "Abnormal Psychology",
        release_date: "2022-01-18T18:27:53.084Z",
        cover_img_url: "example.com",
        category: "testing",
        description: "description words",
        table_of_contents: TOC_ARRAY
      },
      {
        root_outcome_guid: "0a990cb3-8cde-4e9b-a99a-b4caaea82eee2",
        book_title: "Accounting for managers",
        release_date: "2022-01-18T18:27:53.084Z",
        cover_img_url: "example.com",
        category: "testing",
        description: "description words",
        table_of_contents: TOC_ARRAY
      },
      {
        root_outcome_guid: "0a990cb3-8cde-4e9b-a99a-b4caaea82eee3",
        book_title: "Biology for Majors",
        release_date: "2022-01-18T18:27:53.084Z",
        cover_img_url: "example.com",
        category: "testing",
        description: "description words",
        table_of_contents: TOC_ARRAY
      },
      {
        root_outcome_guid: "0a990cb3-8cde-4e9b-a99a-b4caaea82eee",
        book_title: "Biology for Majors II",
        release_date: "2022-01-18T18:27:53.084Z",
        cover_img_url: "example.com",
        category: "testing",
        description: "description words",
        table_of_contents: TOC_ARRAY
      },
      {
        root_outcome_guid: "0a990cb3-8cde-4e9b-a99a-b4caaea82eee4",
        book_title: "Biology for Non-Majors",
        release_date: "2022-01-18T18:27:53.084Z",
        cover_img_url: "example.com",
        category: "testing",
        description: "description words",
        table_of_contents: TOC_ARRAY
      },
      {
        root_outcome_guid: "0a990cb3-8cde-4e9b-a99a-b4caaea82eee5",
        book_title: "Human Resources Management - F19",
        release_date: "2022-01-18T18:27:53.084Z",
        cover_img_url: "example.com",
        category: "testing",
        description: "description words",
        table_of_contents: TOC_ARRAY
      }
    ],
    metadata: {
        page: "1",
        per_page: "10",
        page_count: 1,
        total_count: 1,
        links: {
            first: "/service_api/course_catalog?page=1",
            last: "/service_api/course_catalog?page=1"
        }
    }
};
