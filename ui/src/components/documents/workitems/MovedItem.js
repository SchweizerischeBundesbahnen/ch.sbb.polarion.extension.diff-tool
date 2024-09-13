import {LEFT, RIGHT} from "@/components/documents/workitems/WorkItemHeader";

const MOVED_UP = 0;
const MOVED_DOWN = 1;

export default function MovedItem({side, movedOutlineNumber, moveDirection}) {
  return (
      <span>
        {side === LEFT && moveDirection === MOVED_UP
            && <>
              <a href={`#${side}${movedOutlineNumber}`}>moved to {movedOutlineNumber}</a>
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <g id="SVGRepo_bgCarrier" strokeWidth="0"></g>
                <g id="SVGRepo_tracerCarrier" strokeLinecap="round" strokeLinejoin="round"></g>
                <g id="SVGRepo_iconCarrier">
                  <path d="M17 16L15 16C12.7909 16 11 14.2091 11 12L11 7" stroke="#666666" strokeWidth="2"
                        strokeLinecap="round" strokeLinejoin="round"></path>
                  <path d="M8 10L11 7L14 10" stroke="#666666" strokeWidth="2" strokeLinecap="round"
                        strokeLinejoin="round"></path>
                </g>
              </svg>
            </>}
        {side === LEFT && moveDirection === MOVED_DOWN
            && <>
              <a href={`#${side}${movedOutlineNumber}`}>moved to {movedOutlineNumber}</a>
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <g id="SVGRepo_bgCarrier" strokeWidth="0"></g>
                <g id="SVGRepo_tracerCarrier" strokeLinecap="round" strokeLinejoin="round"></g>
                <g id="SVGRepo_iconCarrier">
                  <path d="M17 8L15 8C12.7909 8 11 9.79086 11 12L11 17" stroke="#666666" strokeWidth="2"
                        strokeLinecap="round" strokeLinejoin="round"></path>
                  <path d="M8 14L11 17L14 14" stroke="#666666" strokeWidth="2" strokeLinecap="round"
                        strokeLinejoin="round"></path>
                </g>
              </svg>
            </>}
        {side === RIGHT && moveDirection === MOVED_UP
            && <>
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <g id="SVGRepo_bgCarrier" strokeWidth="0"></g>
                <g id="SVGRepo_tracerCarrier" strokeLinecap="round" strokeLinejoin="round"></g>
                <g id="SVGRepo_iconCarrier">
                  <path d="M7 16L9 16C11.2091 16 13 14.2091 13 12L13 7" stroke="#666666" strokeWidth="2"
                        strokeLinecap="round" strokeLinejoin="round"></path>
                  <path d="M16 10L13 7L10 10" stroke="#666666" strokeWidth="2" strokeLinecap="round"
                        strokeLinejoin="round"></path>
                </g>
              </svg>
              <a href={`#${side}${movedOutlineNumber}`}>moved to {movedOutlineNumber}</a>
            </>}
        {side === RIGHT && moveDirection === MOVED_DOWN
            && <>
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <g id="SVGRepo_bgCarrier" strokeWidth="0"></g>
                <g id="SVGRepo_tracerCarrier" strokeLinecap="round" strokeLinejoin="round"></g>
                <g id="SVGRepo_iconCarrier">
                  <path d="M7 8L9 8C11.2091 8 13 9.79086 13 12L13 17" stroke="#666666" strokeWidth="2"
                        strokeLinecap="round" strokeLinejoin="round"></path>
                  <path d="M16 14L13 17L10 14" stroke="#666666" strokeWidth="2" strokeLinecap="round"
                        strokeLinejoin="round"></path>
                </g>
              </svg>
              <a href={`#${side}${movedOutlineNumber}`}>moved to {movedOutlineNumber}</a>
            </>}

      </span>
  );
}