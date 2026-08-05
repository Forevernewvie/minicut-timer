/// <reference types="nativewind/types" />

import "lucide-react-native";

declare module "lucide-react-native" {
  interface LucideProps {
    color?: string;
    fill?: string;
  }
}

declare module "*.css" {
  const content: any;
  export default content;
}
