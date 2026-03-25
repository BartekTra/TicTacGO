import React from "react";

interface InputFieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  id: string;
}

export const InputField = ({ label, id, ...props }: InputFieldProps) => {
  return (
    <div className="flex flex-col mb-4">
      <label htmlFor={id} className="mb-2 text-sm font-medium text-gray-100">
        {label}
      </label>
      <input
        id={id}
        className="px-4 py-3 text-gray-100 transition-colors duration-200 border border-gray-300 rounded-md outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
        {...props}
      />
    </div>
  );
};
