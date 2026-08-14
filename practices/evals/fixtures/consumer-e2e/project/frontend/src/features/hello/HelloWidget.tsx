import { Button } from "@/components/Button";
import { greet } from "@/lib/utils";

export function HelloWidget() {
  return (
    <div>
      <p>{greet("world")}</p>
      <Button label="Say hello" />
    </div>
  );
}
