import { gql } from "@apollo/client";

export const HANDLE_MOVE = gql`
  mutation GameMove($cell: Int!) {
    gameMove(cell: $cell) {
      message
      success
    }
  }
`;
