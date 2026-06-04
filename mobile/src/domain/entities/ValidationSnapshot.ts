export type ValidationSnapshot = {
  dslPolicies: number;
  xmiPolicies: number;
  policySets: number;
  missingDslPoliciesInXmi: number;
  missingRequiredAttributes: number;
  engineTests: number;
  dslTests: number;
  lineCoverage: string;
  branchCoverage: string;
};
