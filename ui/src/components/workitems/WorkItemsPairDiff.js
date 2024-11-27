import MergeTicker from "@/components/documents/workitems/MergeTicker";
import {LEFT, RIGHT, WorkItemHeader} from "@/components/documents/workitems/WorkItemHeader";

export default function WorkItemsPairDiff({workItemsPair}) {


  return (
      <>
        <div className={`wi-diff container-fluid g-0`}>
          <div style={{
            backgroundColor: "#f6f6f6",
            display: 'flex'
          }} className="header row g-0">
            <MergeTicker workItemsPair={workItemsPair} diffs={{}} selected={false} pairSelectedCallback={() => {}} />
            <WorkItemHeader workItem={workItemsPair.leftWorkItem} side={LEFT} selected={false}/>
            <WorkItemHeader workItem={workItemsPair.rightWorkItem} side={RIGHT} selected={false}/>
          </div>
        </div>
      </>
  )
}
