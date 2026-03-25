import React from "react";

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  children: React.ReactNode;
  variant?: "primary" | "secondary";
}

export const Button = ({
  children,
  variant = "primary",
  ...props
}: ButtonProps) => {
  const baseClasses =
    "w-full px-4 py-3 text-base font-semibold transition-colors duration-200 rounded-md cursor-pointer outline-none focus:ring-2 focus:ring-offset-1";

  const variantClasses =
    variant === "primary"
      ? "text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-500"
      : "text-gray-700 bg-gray-200 hover:bg-gray-300 focus:ring-gray-400";

  return (
    <button className={`${baseClasses} ${variantClasses}`} {...props}>
      {children}
    </button>
  );
};
