import {useContext, useEffect, useState} from "react";
import AppContext from "@/components/AppContext";
import {usePathname, useRouter, useSearchParams} from "next/navigation";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {faSquarePlus} from "@fortawesome/free-solid-svg-icons";
import Loading from "@/components/loading/Loading";
import AppAlert from "@/components/AppAlert";
import useDiffService from "@/services/useDiffService";
import DocumentsDiff from "@/components/documents/DocumentsDiff";
import DocumentHeader from "@/components/documents/DocumentHeader";
import CollectionHeader from "@/components/collections/CollectionHeader";
import useRemote from "@/services/useRemote";
import Modal from "@/components/Modal";
import DocumentsFieldsDiff from "@/components/documents/DocumentsFieldsDiff";

const REQUIRED_PARAMS = ['sourceProjectId', 'sourceCollectionId', 'targetProjectId', 'targetCollectionId'];

export default function CollectionsDiff() {
  const context = useContext(AppContext);
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const diffService = useDiffService();
  const remote = useRemote();

  const [compareAsWorkItems] = useState(searchParams.get('compareAs') === 'Workitems');
  const [loading, setLoading] = useState(true);
  const [loadingError, setLoadingError] = useState(null);
  const [collectionsData, setCollectionsData] = useState({});
  const [pairedDocuments, setPairedDocuments] = useState([]);
  const [leftDocumentLocationPath] = useState(`${searchParams.get("sourceSpaceId")}/${searchParams.get("sourceDocument")}`);
  const [selectedDocumentsPair, setSelectedDocumentsPair] = useState(null);
  const [targetConfigurations, setTargetConfigurations] = useState([]);
  const [targetConfigurationModalVisible, setTargetConfigurationModalVisible] = useState(false);
  const [selectedTargetConfiguration, setSelectedTargetConfiguration] = useState(searchParams.get("config") || 'Default');
  const [documentCreationInProgress, setDocumentCreationInProgress] = useState(false);
  const [documentCreationError, setDocumentCreationError] = useState(null);
  const [documentCreated, setDocumentCreated] = useState(false);

  useEffect(() => {
    const missingParams = REQUIRED_PARAMS.filter(param => !searchParams.get(param));
    if (missingParams.length > 0) {
      setLoading(false);
      setLoadingError(`Following parameters are missing: [${missingParams.join(", ")}]`);
      return;
    }

    diffService.sendCollectionsDiffRequest(searchParams)
        .then((data) => {
          setCollectionsData(data);
          setPairedDocuments(data.pairedDocuments);
          setLoading(false);
          context.state.setDataLoaded(true);
        }).catch((error) => {
          setLoading(false);
          setLoadingError(error && error.message);
        });

    remote.sendRequest({
      method: "GET",
      url: `/settings/diff/names?scope=project/${searchParams.get("targetProjectId")}/`,
      contentType: "text/html"
    }).then(response => {
      if (response.ok) {
        return response.text();
      } else {
        throw response.json();
      }
    }).then(data => {
      setTargetConfigurations(JSON.parse(data).map(setting => setting.name));
    }).catch(errorResponse => {
      Promise.resolve(errorResponse).then((error) => alert("Error occurred loading setting names of target project" + (error && error.message ? ": " + error.message : "")));
    });
  }, []);

  useEffect(() => {
    if (pairedDocuments) {
      context.state.setLeftCollectionDocuments(pairedDocuments.map(pair => pair.leftDocument));

      if (leftDocumentLocationPath) {
        const documentsPair = pairedDocuments.find(pair => pair.leftDocument.locationPath === leftDocumentLocationPath);

        if (!(searchParams.has("targetSpaceId") && searchParams.has("targetDocument")) && documentsPair && documentsPair.rightDocument) {

          const params = [];
          for (const [key, value] of searchParams.entries()) {
            if (key !== "targetSpaceId" && key !== "targetDocument") {
              params.push(`${key}=${value}`);
            }
          }

          params.push(`targetSpaceId=${documentsPair.rightDocument.spaceId}`);
          params.push(`targetDocument=${documentsPair.rightDocument.id}`);
          router.push(pathname + '?' + params.join('&'));
        }

        setSelectedDocumentsPair(documentsPair);
      }
    } else {
      context.state.setLeftCollectionDocuments([]);
    }
  }, [pairedDocuments]);

  const createTargetDocument = (targetConfiguration) => {
    setDocumentCreationError(null);
    setDocumentCreationInProgress(true);
    diffService.sendCreateTargetDocumentRequest(selectedDocumentsPair.leftDocument, collectionsData.rightCollection.projectId, targetConfiguration, searchParams.get('linkRole'))
        .then(() => {
          setDocumentCreated(true);
        })
        .catch((error) => {
          console.log(error);
          setDocumentCreationError(error && (typeof error === 'string' || error instanceof String) && !error.includes("<html")
              ? error : "Error occurred creating target document, please contact system administrator to diagnose the problem");
        }).finally(() => setDocumentCreationInProgress(false));
  };

  if (loading) return <Loading message="Loading Collections content" />;

  if (loadingError || !collectionsData || !collectionsData.leftCollection || !collectionsData.rightCollection || !collectionsData.pairedDocuments) {
    return <AppAlert title="Error occurred loading diff data!"
                     message={loadingError && !loadingError.includes("<html")
                      ? loadingError
                      : "Data wasn't loaded, please contact system administrator to diagnose the problem"} />;
  }

  if (!(selectedDocumentsPair && selectedDocumentsPair.leftDocument)) {
    return <AppAlert title="No source document selected"
                     message="Please, select source document in side pane first" />;
  }

  if (!selectedDocumentsPair.rightDocument) {
    return <div id="diff-body" className={`doc-diff ${context.state.controlPaneExpanded ? "control-pane-expanded" : ""}`}>
      <div className={`header container-fluid g-0 sticky-top ${context.state.headerPinned ? "pinned" : ""}`}>
        <div className="row g-0">
          <CollectionHeader collection={collectionsData.leftCollection} />
          <CollectionHeader collection={collectionsData.rightCollection} />
        </div>
        <div className="row g-0">
          <DocumentHeader document={selectedDocumentsPair.leftDocument} />
          <div className="col collapsed-border" style={{
            color: "#bbb",
            fontWeight: "200"
          }}>
            No counterpart document in target collection. Do you want to create it?
            <button className="btn btn-secondary btn-xs create-document-button" onClick={() => setTargetConfigurationModalVisible(true)}>
              <FontAwesomeIcon icon={faSquarePlus} style={{ color: "#189418", marginRight: "4px", fontWeight: "600" }}/> Create document
            </button>
          </div>
        </div>
      </div>
      <Modal title="Choose document configuration" visible={targetConfigurationModalVisible} setVisible={setTargetConfigurationModalVisible} className="modal-md"
             cancelButtonTitle="Cancel" actionButtonTitle="Create Document" actionButtonHandler={() => {
               setTargetConfigurationModalVisible(false);
               createTargetDocument(selectedTargetConfiguration);
             }
      }>
        <p>To create target document please choose configuration with which it should be created:</p>
        <div style={{
          display: "flex",
          alignItems: "center",
          columnGap: "20px"
        }} className="select-set">
          <label htmlFor="target-configuration">
            Available configurations:
          </label>
          <select id="target-configuration" className="form-select" style={{ width: "auto"}} value={selectedTargetConfiguration}
                  onChange={event => setSelectedTargetConfiguration(event.target.value)}>
            {targetConfigurations.map((configuration, index) => {
              return <option key={index} value={configuration}>{configuration}</option>
            })}
          </select>
        </div>
      </Modal>
      {documentCreationInProgress && <Loading message="Creating target document"/>}
      {documentCreationError
          && <AppAlert title="Error occurred creating target document!"
                       message={documentCreationError.includes("already exists")
                           ? documentCreationError + ". Please add the document to the target collection manually and repeat comparison."
                           : documentCreationError}/>}
      {documentCreated && <AppAlert title="Target document successfully created"
                                    message="Target document successfully created. Please, add it manually to target collection and repeat comparison."/>}
    </div>;
  }

  if (compareAsWorkItems) {
    return <DocumentsDiff enclosingCollections={collectionsData} />;
  } else {
    return <DocumentsFieldsDiff enclosingCollections={collectionsData} />;
  }
}
